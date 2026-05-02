package com.chatapp.chatservice.websocket;

import com.chatapp.chatservice.config.JwtUtils;
import com.chatapp.chatservice.dto.BroadcastMessage;
import com.chatapp.chatservice.dto.InboundChatMessage;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.MessageStatus;
import com.chatapp.chatservice.dto.MessageSentEvent;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.service.RoomServiceClient;
import com.chatapp.chatservice.service.RoomServiceClient.RoomServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_ROOM_ID  = "roomId";
    private static final String ATTR_USER_ID  = "userId";
    private static final String ATTR_USERNAME = "username";
    private static final String KAFKA_TOPIC   = "chat.messages";

    private final JwtUtils jwtUtils;
    private final RoomServiceClient roomServiceClient;
    private final RoomSessionRegistry sessionRegistry;
    private final MessageRepository messageRepository;
    private final KafkaTemplate<String, MessageSentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Counter messagesSentCounter;
    private final AtomicInteger activeWebSocketConnections;

    // ── Connection lifecycle ────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token  = extractToken(session);
        UUID   roomId = extractRoomId(session);

        // 1. Validate JWT
        if (token == null || !jwtUtils.isValid(token)) {
            log.warn("WS connect rejected — invalid token, sessionId={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
            return;
        }

        UUID   userId   = jwtUtils.extractUserId(token);
        String username = jwtUtils.extractUsername(token);

        // 2. Verify room membership via Room Service
        try {
            if (!roomServiceClient.isMember(roomId, userId)) {
                log.warn("WS connect rejected — user {} not a member of room {}", userId, roomId);
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Not a room member"));
                return;
            }
        } catch (RoomServiceUnavailableException e) {
            log.error("WS connect rejected — Room Service unavailable for roomId={}", roomId);
            session.close(new CloseStatus(1013, "Room Service unavailable"));
            return;
        }

        // 3. Store identity in session attributes and register
        session.getAttributes().put(ATTR_ROOM_ID, roomId);
        session.getAttributes().put(ATTR_USER_ID, userId);
        session.getAttributes().put(ATTR_USERNAME, username);

        sessionRegistry.register(roomId, session);
        activeWebSocketConnections.incrementAndGet();

        log.info("WS connection accepted roomId={} userId={} sessionId={}", roomId, userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID roomId = (UUID) session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId != null) {
            sessionRegistry.deregister(roomId, session);
            activeWebSocketConnections.decrementAndGet();
            log.info("WS connection closed roomId={} sessionId={} status={}", roomId, session.getId(), status);
        }
    }

    // ── Message handling ────────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        UUID roomId   = (UUID) session.getAttributes().get(ATTR_ROOM_ID);
        UUID userId   = (UUID) session.getAttributes().get(ATTR_USER_ID);
        String username = (String) session.getAttributes().get(ATTR_USERNAME);

        if (roomId == null || userId == null) {
            log.warn("Received message from unauthenticated session {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        InboundChatMessage inbound;
        try {
            inbound = objectMapper.readValue(textMessage.getPayload(), InboundChatMessage.class);
        } catch (Exception e) {
            log.warn("Failed to parse inbound message from sessionId={}: {}", session.getId(), e.getMessage());
            return;
        }

        if (inbound.getContent() == null || inbound.getContent().isBlank()) {
            return;
        }

        // 1. Persist
        UUID    messageId = UUID.randomUUID();
        Instant now       = Instant.now();

        Message message = Message.builder()
                .id(messageId)
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .content(inbound.getContent())
                .status(MessageStatus.ACTIVE)
                .sentAt(now)
                .build();
        messageRepository.save(message);

        // 2. Broadcast to room
        BroadcastMessage broadcast = BroadcastMessage.builder()
                .messageId(messageId)
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .content(inbound.getContent())
                .status("ACTIVE")
                .sentAt(now)
                .build();

        String broadcastJson = objectMapper.writeValueAsString(broadcast);
        sessionRegistry.broadcast(roomId, broadcastJson);

        // 3. Publish Kafka event
        MessageSentEvent event = MessageSentEvent.builder()
                .messageId(messageId)
                .roomId(roomId)
                .userId(userId)
                .content(inbound.getContent())
                .timestamp(now)
                .build();

        kafkaTemplate.send(KAFKA_TOPIC, roomId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish MessageSent to Kafka messageId={}: {}", messageId, ex.getMessage());
                    }
                });

        messagesSentCounter.increment();
        log.debug("Message sent roomId={} messageId={}", roomId, messageId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error sessionId={}: {}", session.getId(), exception.getMessage());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private UUID extractRoomId(WebSocketSession session) {
        // Path: /ws/rooms/{roomId}
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        // parts = ["", "ws", "rooms", "{roomId}"]
        try {
            return UUID.fromString(parts[parts.length - 1]);
        } catch (Exception e) {
            log.error("Cannot extract roomId from path: {}", path);
            return null;
        }
    }
}

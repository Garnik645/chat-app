package com.chatapp.chatservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry that maps roomId → set of active WebSocket sessions.
 * Also exposes total active connection count for Prometheus gauge.
 */
@Slf4j
@Component
public class RoomSessionRegistry {

    // roomId → set of sessions
    private final Map<UUID, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public void register(UUID roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session registered roomId={} sessionId={}", roomId, session.getId());
    }

    public void deregister(UUID roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        log.debug("Session deregistered roomId={} sessionId={}", roomId, session.getId());
    }

    public void broadcast(UUID roomId, String message) {
        Set<WebSocketSession> sessions = roomSessions.getOrDefault(roomId, Collections.emptySet());
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("Failed to send message to sessionId={}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    public int totalActiveSessions() {
        return roomSessions.values().stream().mapToInt(Set::size).sum();
    }
}

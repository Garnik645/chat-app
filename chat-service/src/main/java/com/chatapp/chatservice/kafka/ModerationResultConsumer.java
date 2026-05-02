package com.chatapp.chatservice.kafka;

import com.chatapp.chatservice.dto.MessageModeratedEvent;
import com.chatapp.chatservice.dto.MessageRemovedNotification;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.MessageStatus;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.websocket.RoomSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationResultConsumer {

    private final MessageRepository messageRepository;
    private final RoomSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final Counter messagesRejectedCounter;

    @KafkaListener(topics = "moderation.results",
                   groupId = "chat-service-moderation-consumer",
                   containerFactory = "moderationKafkaListenerContainerFactory")
    public void onModerationResult(MessageModeratedEvent event) {
        log.debug("Received moderation result messageId={} verdict={}", event.getMessageId(), event.getVerdict());

        if (!"REJECTED".equals(event.getVerdict())) {
            // APPROVED — nothing to do
            return;
        }

        Optional<Message> optMessage = messageRepository.findById(event.getMessageId());
        if (optMessage.isEmpty()) {
            log.warn("Moderation REJECTED for unknown messageId={}", event.getMessageId());
            return;
        }

        Message message = optMessage.get();
        message.setStatus(MessageStatus.REJECTED);
        message.setModeratedAt(Instant.now());
        messageRepository.save(message);

        messagesRejectedCounter.increment();
        log.info("Message REJECTED by moderation messageId={} roomId={}", message.getId(), message.getRoomId());

        // Broadcast removal notification to all clients in the room
        try {
            MessageRemovedNotification notification = new MessageRemovedNotification(message.getId(), "MODERATION");
            String json = objectMapper.writeValueAsString(notification);
            sessionRegistry.broadcast(message.getRoomId(), json);
        } catch (Exception e) {
            log.error("Failed to broadcast MESSAGE_REMOVED notification for messageId={}: {}",
                    message.getId(), e.getMessage());
        }
    }
}

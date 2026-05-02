package com.chatapp.chatservice.service;

import com.chatapp.chatservice.dto.ChatHistoryResponse;
import com.chatapp.chatservice.dto.MessageDto;
import com.chatapp.chatservice.dto.MessageRemovedNotification;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.MessageStatus;
import com.chatapp.chatservice.repository.MessageRepository;
import com.chatapp.chatservice.websocket.RoomSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final RoomServiceClient roomServiceClient;
    private final RoomSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Returns paginated ACTIVE messages for a room, newest first.
     * Throws 404 if the room has no messages at all (room doesn't exist in our domain).
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(UUID roomId, int page, int size) {
        // Clamp size to max 100
        int clampedSize = Math.min(size, 100);

        // We check whether any messages exist for this roomId to detect invalid rooms.
        // Per spec, return 404 if the room does not exist.
        // Since Chat Service doesn't own room data, we treat "no record of this room" as 404.
//        if (!messageRepository.existsByRoomId(roomId)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
//        }

        PageRequest pageRequest = PageRequest.of(page, clampedSize);
        Page<Message> messagePage = messageRepository
                .findByRoomIdAndStatusOrderBySentAtDesc(roomId, MessageStatus.ACTIVE, pageRequest);

        List<MessageDto> dtos = messagePage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .content(dtos)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .build();
    }

    /**
     * Deletes a message as a room admin.
     * - Verifies the message exists (404 if not).
     * - Verifies the requesting user is the room admin via Room Service (403 if not).
     * - Sets status to DELETED and broadcasts a MESSAGE_REMOVED notification.
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID requestingUserId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        // Verify admin via Room Service
        boolean isAdmin;
        try {
            isAdmin = roomServiceClient.isRoomAdmin(message.getRoomId(), requestingUserId);
        } catch (RoomServiceClient.RoomServiceUnavailableException e) {
            log.error("Room Service unavailable during delete message={} userId={}", messageId, requestingUserId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Room Service unavailable");
        }

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room admin can delete messages");
        }

        message.setStatus(MessageStatus.DELETED);
        messageRepository.save(message);
        log.info("Message DELETED by admin messageId={} adminUserId={}", messageId, requestingUserId);

        // Broadcast removal notification
        try {
            MessageRemovedNotification notification =
                    new MessageRemovedNotification(messageId, "ADMIN_DELETE");
            String json = objectMapper.writeValueAsString(notification);
            sessionRegistry.broadcast(message.getRoomId(), json);
        } catch (Exception e) {
            log.error("Failed to broadcast ADMIN_DELETE notification for messageId={}: {}", messageId, e.getMessage());
        }
    }

    private MessageDto toDto(Message message) {
        return MessageDto.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .userId(message.getUserId())
                .username(message.getUsername())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}

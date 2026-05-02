package com.chatapp.chatservice.controller;

import com.chatapp.chatservice.dto.ChatHistoryResponse;
import com.chatapp.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    /**
     * GET /api/messages/{roomId}?page=0&size=50
     *
     * Returns paginated chat history for a room (ACTIVE messages only, newest first).
     * Identity is provided by the gateway via X-User-Id header (already validated).
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("GET chat history roomId={} page={} size={}", roomId, page, size);
        ChatHistoryResponse response = chatService.getChatHistory(roomId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/messages/{messageId}
     *
     * Allows a room admin to delete a message.
     * The requesting user's ID is read from the X-User-Id header forwarded by the gateway.
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID messageId,
            @RequestHeader("X-User-Id") UUID userId) {

        log.debug("DELETE message messageId={} requestedBy={}", messageId, userId);
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }
}

package com.chatapp.chatservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastMessage {
    private UUID messageId;
    private UUID roomId;
    private UUID userId;
    private String username;
    private String content;
    private String status;
    private Instant sentAt;
}

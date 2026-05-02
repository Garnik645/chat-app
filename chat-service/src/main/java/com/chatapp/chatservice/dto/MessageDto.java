package com.chatapp.chatservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private UUID messageId;
    private UUID roomId;
    private UUID userId;
    private String username;
    private String content;
    private Instant sentAt;
}

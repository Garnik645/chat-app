package com.chatapp.chatservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageModeratedEvent {
    private UUID messageId;
    private UUID roomId;
    private String verdict;
    private Instant timestamp;
}

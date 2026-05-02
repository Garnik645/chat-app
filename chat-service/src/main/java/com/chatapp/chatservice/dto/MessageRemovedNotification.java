package com.chatapp.chatservice.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRemovedNotification {
    private final String type = "MESSAGE_REMOVED";
    private UUID messageId;
    private String reason;
}

package com.chatapp.chatservice.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private List<MessageDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

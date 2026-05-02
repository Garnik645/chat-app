package com.chat.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private Instant timestamp;
    private Map<String, String> fieldErrors;
}

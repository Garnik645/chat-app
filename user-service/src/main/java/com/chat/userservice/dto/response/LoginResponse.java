package com.chat.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {

    private String token;
    private UUID userId;
    private String username;
    private String role;
}

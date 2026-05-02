package com.chat.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private Instant createdAt;
}

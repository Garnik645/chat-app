package com.chat.userservice.mapper;

import com.chat.userservice.dto.response.RegisterResponse;
import com.chat.userservice.dto.response.UserResponse;
import com.chat.userservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public RegisterResponse toRegisterResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

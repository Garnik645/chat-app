package com.chat.userservice.service;

import com.chat.userservice.dto.request.LoginRequest;
import com.chat.userservice.dto.request.RegisterRequest;
import com.chat.userservice.dto.response.LoginResponse;
import com.chat.userservice.dto.response.RegisterResponse;
import com.chat.userservice.dto.response.UserResponse;
import com.chat.userservice.entity.User;
import com.chat.userservice.exception.ConflictException;
import com.chat.userservice.exception.UserNotFoundException;
import com.chat.userservice.mapper.UserMapper;
import com.chat.userservice.repository.UserRepository;
import com.chat.userservice.security.JwtService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final Counter usersRegisteredCounter;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role("ROLE_USER")
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, username={}", saved.getId(), saved.getUsername());

        usersRegisteredCounter.increment();

        return userMapper.toRegisterResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userIdHeader) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException("Invalid user ID in request header");
        }

        return getUserById(userId);
    }
}

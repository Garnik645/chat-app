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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private Counter usersRegisteredCounter;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .firstName("Test")
                .lastName("User")
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .build();
    }

    // --- register ---

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toRegisterResponse(sampleUser)).thenReturn(
                RegisterResponse.builder()
                        .id(sampleUser.getId())
                        .username("testuser")
                        .email("test@example.com")
                        .firstName("Test")
                        .lastName("User")
                        .createdAt(sampleUser.getCreatedAt())
                        .build());

        RegisterResponse result = userService.register(request);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(usersRegisteredCounter).increment();
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("other@example.com");
        request.setPassword("password123");
        request.setFirstName("A");
        request.setLastName("B");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("testuser");

        verify(usersRegisteredCounter, never()).increment();
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("A");
        request.setLastName("B");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("test@example.com");
    }

    // --- login ---

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("jwt-token");

        LoginResponse result = userService.login(request);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_unknownUser_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("password123");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // --- getUserById ---

    @Test
    void getUserById_found() {
        UUID id = sampleUser.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserResponse(sampleUser)).thenReturn(
                UserResponse.builder()
                        .id(id)
                        .username("testuser")
                        .firstName("Test")
                        .lastName("User")
                        .createdAt(sampleUser.getCreatedAt())
                        .build());

        UserResponse result = userService.getUserById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserById_notFound_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(UserNotFoundException.class);
    }
}

package com.chatapp.chatservice;

import com.chatapp.chatservice.service.ChatService;
import com.chatapp.chatservice.entity.MessageStatus;
import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "jwt.secret=test-secret-key-for-unit-tests-only-32c",
        "room-service.url=http://localhost:8082"
})
class ChatServiceApplicationTests {

    @Autowired
    private ChatService chatService;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Test
    void contextLoads() {
        assertThat(chatService).isNotNull();
    }

    @Test
    void getChatHistory_roomNotFound_throws404() {
        UUID roomId = UUID.randomUUID();
        when(messageRepository.existsByRoomId(roomId)).thenReturn(false);

        assertThatThrownBy(() -> chatService.getChatHistory(roomId, 0, 50))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getChatHistory_roomExists_returnsPage() {
        UUID roomId = UUID.randomUUID();
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .roomId(roomId)
                .userId(UUID.randomUUID())
                .username("alice")
                .content("hello")
                .status(MessageStatus.ACTIVE)
                .sentAt(Instant.now())
                .build();

        when(messageRepository.existsByRoomId(roomId)).thenReturn(true);
        when(messageRepository.findByRoomIdAndStatusOrderBySentAtDesc(
                eq(roomId), eq(MessageStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(msg)));

        var result = chatService.getChatHistory(roomId, 0, 50);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
    }
}

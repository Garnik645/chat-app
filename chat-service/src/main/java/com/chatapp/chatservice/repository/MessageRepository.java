package com.chatapp.chatservice.repository;

import com.chatapp.chatservice.entity.Message;
import com.chatapp.chatservice.entity.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByRoomIdAndStatusOrderBySentAtDesc(UUID roomId, MessageStatus status, Pageable pageable);

    boolean existsByRoomId(UUID roomId);
}

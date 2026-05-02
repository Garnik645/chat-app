package com.example.roomservice.repository;

import com.example.roomservice.entity.RoomMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    Page<RoomMember> findAllByRoomId(UUID roomId, Pageable pageable);

    void deleteAllByRoomId(UUID roomId);

    long countByRoomId(UUID roomId);

    long countByUserId(UUID userId);

    long count();
}

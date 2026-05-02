package com.example.roomservice.service;

import com.example.roomservice.dto.RoomDto.*;
import com.example.roomservice.entity.Room;
import com.example.roomservice.entity.RoomMember;
import com.example.roomservice.exception.*;
import com.example.roomservice.repository.RoomMemberRepository;
import com.example.roomservice.repository.RoomRepository;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final Counter roomsCreatedCounter;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       Counter roomsCreatedCounter) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomsCreatedCounter = roomsCreatedCounter;
    }

    // ── Create Room ──────────────────────────────────────────────────────────

    public RoomResponse createRoom(CreateRoomRequest request, UUID creatorId) {
        if (roomRepository.existsByName(request.name())) {
            throw new RoomNameConflictException("A room with name '" + request.name() + "' already exists");
        }

        Room room = new Room();
        room.setName(request.name());
        room.setDescription(request.description());
        room.setCreatedBy(creatorId);
        room = roomRepository.save(room);

        // Creator is automatically added as a member
        addMember(room.getId(), creatorId);

        roomsCreatedCounter.increment();
        log.info("Room created: id={}, name={}, creator={}", room.getId(), room.getName(), creatorId);

        return toRoomResponse(room);
    }

    // ── List Rooms ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<RoomSummaryResponse> listRooms(int page, int size) {
        size = Math.min(size, 100);
        Page<Room> roomPage = roomRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        var content = roomPage.getContent().stream()
                .map(r -> new RoomSummaryResponse(
                        r.getId(), r.getName(), r.getDescription(), r.getCreatedBy(), r.getCreatedAt(),
                        roomMemberRepository.countByRoomId(r.getId())))
                .toList();

        return new PagedResponse<>(content, roomPage.getNumber(), roomPage.getSize(),
                roomPage.getTotalElements(), roomPage.getTotalPages());
    }

    // ── Get Room by ID ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RoomSummaryResponse getRoomById(UUID roomId) {
        Room room = findRoomOrThrow(roomId);
        long memberCount = roomMemberRepository.countByRoomId(roomId);
        return new RoomSummaryResponse(room.getId(), room.getName(), room.getDescription(),
                room.getCreatedBy(), room.getCreatedAt(), memberCount);
    }

    // ── Join Room ────────────────────────────────────────────────────────────

    public MembershipResponse joinRoom(UUID roomId, UUID userId) {
        findRoomOrThrow(roomId);

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new AlreadyMemberException("User is already a member of this room");
        }

        RoomMember member = addMember(roomId, userId);
        log.info("User {} joined room {}", userId, roomId);

        return new MembershipResponse(member.getRoomId(), member.getUserId(), member.getJoinedAt());
    }

    // ── Check Membership ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CheckMembershipResponse checkMembership(UUID roomId, UUID userId) {
        findRoomOrThrow(roomId);
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
        return new CheckMembershipResponse(isMember);
    }

    // ── Remove Member ────────────────────────────────────────────────────────

    public void removeMember(UUID roomId, UUID requestingUserId, UUID targetUserId) {
        Room room = findRoomOrThrow(roomId);

        if (!room.getCreatedBy().equals(requestingUserId)) {
            throw new ForbiddenException("Only the room admin can remove members");
        }

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new MemberNotFoundException(
                        "User " + targetUserId + " is not a member of room " + roomId));

        roomMemberRepository.delete(member);
        log.info("User {} removed from room {} by admin {}", targetUserId, roomId, requestingUserId);
    }

    // ── List Room Members ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<MemberResponse> listRoomMembers(UUID roomId, int page, int size) {
        findRoomOrThrow(roomId);
        size = Math.min(size, 100);

        Page<RoomMember> memberPage = roomMemberRepository.findAllByRoomId(
                roomId, PageRequest.of(page, size, Sort.by("joinedAt").ascending()));

        var content = memberPage.getContent().stream()
                .map(m -> new MemberResponse(m.getUserId(), m.getJoinedAt()))
                .toList();

        return new PagedResponse<>(content, memberPage.getNumber(), memberPage.getSize(),
                memberPage.getTotalElements(), memberPage.getTotalPages());
    }

    // ── Delete Room ──────────────────────────────────────────────────────────

    public void deleteRoom(UUID roomId, UUID requestingUserId) {
        Room room = findRoomOrThrow(roomId);

        if (!room.getCreatedBy().equals(requestingUserId)) {
            throw new ForbiddenException("Only the room creator can delete this room");
        }

        roomMemberRepository.deleteAllByRoomId(roomId);
        roomRepository.delete(room);
        log.info("Room {} deleted by creator {}", roomId, requestingUserId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Room findRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
    }

    private RoomMember addMember(UUID roomId, UUID userId) {
        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        return roomMemberRepository.save(member);
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getDescription(),
                room.getCreatedBy(), room.getCreatedAt());
    }
}

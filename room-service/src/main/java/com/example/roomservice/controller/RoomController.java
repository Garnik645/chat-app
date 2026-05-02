package com.example.roomservice.controller;

import com.example.roomservice.dto.RoomDto.*;
import com.example.roomservice.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * All endpoints are protected at the API Gateway level.
 * The authenticated user's ID is read from the X-User-Id header forwarded by the gateway.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * POST /api/rooms — Create a new room.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return roomService.createRoom(request, userId);
    }

    /**
     * GET /api/rooms — Paginated list of all rooms.
     */
    @GetMapping
    public PagedResponse<RoomSummaryResponse> listRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.listRooms(page, size);
    }

    /**
     * GET /api/rooms/{roomId} — Get a specific room by ID.
     */
    @GetMapping("/{roomId}")
    public RoomSummaryResponse getRoom(@PathVariable UUID roomId) {
        return roomService.getRoomById(roomId);
    }

    /**
     * DELETE /api/rooms/{roomId} — Delete a room (creator only).
     */
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(
            @PathVariable UUID roomId,
            @RequestHeader("X-User-Id") UUID userId) {
        roomService.deleteRoom(roomId, userId);
    }

    /**
     * POST /api/rooms/{roomId}/members — Join a room.
     */
    @PostMapping("/{roomId}/members")
    public MembershipResponse joinRoom(
            @PathVariable UUID roomId,
            @RequestHeader("X-User-Id") UUID userId) {
        return roomService.joinRoom(roomId, userId);
    }

    /**
     * GET /api/rooms/{roomId}/members/{userId} — Check if a user is a member (used internally by Chat Service).
     */
    @GetMapping("/{roomId}/members/{userId}")
    public CheckMembershipResponse checkMembership(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        return roomService.checkMembership(roomId, userId);
    }

    /**
     * DELETE /api/rooms/{roomId}/members/{userId} — Remove a member (admin only).
     */
    @DeleteMapping("/{roomId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") UUID requestingUserId) {
        roomService.removeMember(roomId, requestingUserId, userId);
    }

    /**
     * GET /api/rooms/{roomId}/members — Paginated list of room members.
     */
    @GetMapping("/{roomId}/members")
    public PagedResponse<MemberResponse> listRoomMembers(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.listRoomMembers(roomId, page, size);
    }
}

package com.example.roomservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class RoomDto {

    // ── Requests ──────────────────────────────────────────────────────────────

    public record CreateRoomRequest(
            @NotBlank(message = "name must not be blank")
            @Size(max = 100, message = "name must not exceed 100 characters")
            String name,

            @Size(max = 500, message = "description must not exceed 500 characters")
            String description
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record RoomResponse(
            UUID id,
            String name,
            String description,
            UUID createdBy,
            OffsetDateTime createdAt
    ) {}

    public record RoomSummaryResponse(
            UUID id,
            String name,
            String description,
            UUID createdBy,
            OffsetDateTime createdAt,
            long memberCount
    ) {}

    public record MembershipResponse(
            UUID roomId,
            UUID userId,
            OffsetDateTime joinedAt
    ) {}

    public record MemberResponse(
            UUID userId,
            OffsetDateTime joinedAt
    ) {}

    public record CheckMembershipResponse(
            @JsonProperty("isMember")
            boolean isMember
    ) {}

    // ── Pagination ────────────────────────────────────────────────────────────

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}

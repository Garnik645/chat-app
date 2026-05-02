package com.example.roomservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    private void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}

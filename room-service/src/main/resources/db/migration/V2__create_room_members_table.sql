CREATE TABLE room_members
(
    id        UUID                     NOT NULL,
    room_id   UUID                     NOT NULL,
    user_id   UUID                     NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_room_members PRIMARY KEY (id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE UNIQUE INDEX idx_room_members_room_user
    ON room_members (room_id, user_id);

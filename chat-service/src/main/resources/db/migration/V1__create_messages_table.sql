CREATE TABLE messages
(
    id           UUID        NOT NULL,
    room_id      UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    username     VARCHAR(50) NOT NULL,
    content      TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sent_at      TIMESTAMP   NOT NULL,
    moderated_at TIMESTAMP,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE INDEX idx_messages_room_id_sent_at
    ON messages (room_id, sent_at DESC);

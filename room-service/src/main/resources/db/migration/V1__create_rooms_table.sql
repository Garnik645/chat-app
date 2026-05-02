CREATE TABLE rooms
(
    id          UUID                     NOT NULL,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(500),
    created_by  UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uq_rooms_name UNIQUE (name)
);

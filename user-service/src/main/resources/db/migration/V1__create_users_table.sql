CREATE TABLE users
(
    id            UUID         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_username UNIQUE (username),
    CONSTRAINT uc_users_email UNIQUE (email)
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

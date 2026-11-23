-- Domain events written in the same transaction as the trade change, relayed asynchronously.
CREATE TABLE outbox_events (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(64)  NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    published_at   DATETIME(6)  NULL,
    attempts       INT          NOT NULL DEFAULT 0,
    last_error     VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_pending (published_at, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Stored responses for client-supplied Idempotency-Key headers, scoped per user.
CREATE TABLE idempotency_keys (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    idem_key        VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status INT          NOT NULL,
    response_body   MEDIUMTEXT   NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_user_key (user_id, idem_key),
    KEY idx_idempotency_created (created_at),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

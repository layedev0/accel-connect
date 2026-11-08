-- V10: Absence request tables

CREATE TABLE absence_request (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    date            DATE        NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    duration_hours  DECIMAL(4,2) NOT NULL,
    reason          VARCHAR(500),
    status          INT         NOT NULL DEFAULT 1,
    date_applied    DATE        NOT NULL,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT fk_absence_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE absence_request_approvers (
    absence_request_id  INT    NOT NULL,
    user_id             BIGINT NOT NULL,
    PRIMARY KEY (absence_request_id, user_id),
    CONSTRAINT fk_absence_approver_request FOREIGN KEY (absence_request_id) REFERENCES absence_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_absence_approver_user    FOREIGN KEY (user_id)             REFERENCES users(id)
);

CREATE TABLE absence_comment (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    absence_request_id  INT         NOT NULL,
    created             DATETIME(6),
    created_by_id       BIGINT,
    comments            VARCHAR(255),
    CONSTRAINT fk_absence_comment_request FOREIGN KEY (absence_request_id) REFERENCES absence_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_absence_comment_user    FOREIGN KEY (created_by_id)       REFERENCES users(id)
);

-- Join table: leave_request_approvers
-- Users (MANAGER role) who can approve a leave request. Any one of them can approve.
CREATE TABLE leave_request_approvers (
    leave_request_id INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (leave_request_id, user_id),
    CONSTRAINT fk_leave_request_approvers_leave_request FOREIGN KEY (leave_request_id) REFERENCES leave_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_approvers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

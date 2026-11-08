-- Team supervisors: users who have read-only oversight of a team's approved/rejected
-- leave and absence requests, without being the designated approver.
CREATE TABLE team_supervisors (
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (team_id, user_id)
);

-- Register bamba.diouf as supervisor of R&D and Data&AI teams.
-- Safe: SELECT...WHERE produces no rows if teams or user do not yet exist.
INSERT INTO team_supervisors (team_id, user_id)
SELECT t.id, u.id
FROM teams t
CROSS JOIN users u
WHERE t.name IN ('R&D', 'Data&AI')
  AND u.email = 'bamba.diouf@accel-tech.net';

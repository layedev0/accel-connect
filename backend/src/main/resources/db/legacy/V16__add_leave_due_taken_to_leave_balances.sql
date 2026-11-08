ALTER TABLE leave_balances
    ADD COLUMN IF NOT EXISTS leave_due    NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS leave_taken  NUMERIC(19,2) NOT NULL DEFAULT 0;

-- Initialize leave_taken from actual approved/taken leave records
UPDATE leave_balances lb
SET leave_taken = COALESCE((
    SELECT SUM(l.length_days)
    FROM leaves l
    WHERE l.user_id = lb.user_id
      AND l.status IN (2, 3)
      AND l.length_days IS NOT NULL
), 0);

-- Initialize leave_due = current remaining balance + leave_taken
UPDATE leave_balances
SET leave_due = balance_days + leave_taken;

-- V13: Replace leave_type_id FK with leave_type_code VARCHAR (enum-driven approach).
-- Leave types are now defined in LeaveTypeCode enum — no leave_types table needed.

-- 1. Add new code column to both tables
ALTER TABLE leave_request ADD COLUMN leave_type_code VARCHAR(50);
ALTER TABLE leaves        ADD COLUMN leave_type_code VARCHAR(50);

-- 2. Populate from existing leave_types table (default to ANNUAL_LEAVE for unrecognised codes)
UPDATE leave_request lr
SET leave_type_code = COALESCE(
    (SELECT lt.code FROM leave_types lt WHERE lt.id = lr.leave_type_id),
    'ANNUAL_LEAVE'
);

UPDATE leaves l
SET leave_type_code = COALESCE(
    (SELECT lt.code FROM leave_types lt WHERE lt.id = l.leave_type_id),
    'ANNUAL_LEAVE'
);

-- 3. Make NOT NULL
ALTER TABLE leave_request ALTER COLUMN leave_type_code SET NOT NULL;
ALTER TABLE leaves        ALTER COLUMN leave_type_code SET NOT NULL;

-- 4. Drop old FK columns (PostgreSQL automatically drops the associated FK constraint)
ALTER TABLE leave_request DROP COLUMN leave_type_id;
ALTER TABLE leaves        DROP COLUMN leave_type_id;

-- 5. Drop leave_types table — no longer needed
DROP TABLE IF EXISTS leave_types;

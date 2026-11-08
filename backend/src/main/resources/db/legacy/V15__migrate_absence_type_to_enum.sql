-- V15: Replace absence_type FK with enum string column

-- 1. Add new enum column
ALTER TABLE absence_request ADD COLUMN absence_type VARCHAR(50) NULL;

-- 2. Migrate existing data from the FK relationship to enum values
UPDATE absence_request ar
    JOIN absence_type at ON ar.absence_type_id = at.id
SET ar.absence_type = CASE at.code
    WHEN 'SICK'               THEN 'MALADE'
    WHEN 'EARLY_LEAVE'        THEN 'QUITTE_TOT'
    WHEN 'LATE_ARRIVAL'       THEN 'ARRIVE_TARD'
    WHEN 'LONG_LUNCH'         THEN 'LONG_LUNCH'
    WHEN 'FAMILY_EMERGENCY'   THEN 'URGENCE_FAMILIALE'
    WHEN 'APPOINTMENT'        THEN 'RENDEZ_VOUS'
    WHEN 'MEDICAL_APPOINTMENT' THEN 'RENDEZ_VOUS'
    WHEN 'REMOTE_WORK'        THEN 'TELECOMMUTE'
    ELSE 'UNDISCLOSED'
END;

-- 3. Drop FK constraint and old column
ALTER TABLE absence_request DROP FOREIGN KEY fk_absence_request_type;
ALTER TABLE absence_request DROP COLUMN absence_type_id;

-- 4. Drop the absence_type reference table
DROP TABLE absence_type;

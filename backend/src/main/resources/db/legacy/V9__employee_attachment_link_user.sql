-- V9: Link employee_attachment to users instead of employee_details.
--     Renames emp_number → user_id and changes the FK target to users(id).

-- Drop any existing FK on emp_number (name may vary by environment)
DO $$
DECLARE
    v_constraint TEXT;
BEGIN
    SELECT tc.constraint_name
      INTO v_constraint
      FROM information_schema.table_constraints  tc
      JOIN information_schema.key_column_usage   kcu
        ON tc.constraint_name = kcu.constraint_name
       AND tc.table_schema    = kcu.table_schema
     WHERE tc.table_name      = 'employee_attachment'
       AND tc.constraint_type = 'FOREIGN KEY'
       AND kcu.column_name    = 'emp_number'
     LIMIT 1;

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE employee_attachment DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
END $$;

-- Rename the column and widen to BIGINT to match users.id
ALTER TABLE employee_attachment RENAME COLUMN emp_number TO user_id;
ALTER TABLE employee_attachment ALTER COLUMN user_id TYPE BIGINT;

-- Add new FK referencing users
ALTER TABLE employee_attachment
    ADD CONSTRAINT fk_employee_attachment_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

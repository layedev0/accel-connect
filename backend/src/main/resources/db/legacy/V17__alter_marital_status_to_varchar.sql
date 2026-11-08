DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'employee_details'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%marital_status%'
    LOOP
        EXECUTE 'ALTER TABLE employee_details DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE employee_details
    ALTER COLUMN marital_status TYPE VARCHAR(50)
    USING marital_status::text;

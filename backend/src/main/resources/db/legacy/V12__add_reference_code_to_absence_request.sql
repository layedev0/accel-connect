ALTER TABLE absence_request
    ADD COLUMN reference_code VARCHAR(30) UNIQUE;

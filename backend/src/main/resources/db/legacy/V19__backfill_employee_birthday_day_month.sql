-- Birthday-celebration feature: EmployeeDetails now stores the day and month of
-- birth separately (day_birth / month_birth) so upcoming birthdays can be queried
-- without depending on the birth year.
--
-- Flyway runs before Hibernate's ddl-auto, so create the columns here (idempotent)
-- and backfill them from the existing full birthday for records already in the DB.

ALTER TABLE employee_details ADD COLUMN IF NOT EXISTS day_birth INTEGER;
ALTER TABLE employee_details ADD COLUMN IF NOT EXISTS month_birth INTEGER;

UPDATE employee_details
SET day_birth   = EXTRACT(DAY FROM birthday),
    month_birth = EXTRACT(MONTH FROM birthday)
WHERE birthday IS NOT NULL
  AND (day_birth IS NULL OR month_birth IS NULL);

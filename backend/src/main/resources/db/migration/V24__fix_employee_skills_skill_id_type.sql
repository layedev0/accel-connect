-- employee_skills.skill_id was bigint in production while EmployeeSkill maps it as Integer,
-- which makes ddl-auto=validate refuse to start: found [int8], but expecting [integer].
--
-- integer is the right type: skills.id is integer, and EmployeeSkillId.skillId is Integer too.
-- Both employee_skills and skills are empty in production, so the conversion rewrites nothing.
--
-- On databases where Hibernate already created the column as integer, this is a no-op.

ALTER TABLE employee_skills ALTER COLUMN skill_id TYPE integer;

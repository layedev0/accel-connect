-- V14: Remap legacy numeric leave_type_code values to valid LeaveTypeCode enum codes.
-- The V13 migration copied the old leave_types.code values (e.g. '001', '003') verbatim.
-- Any code not already a valid enum value is remapped to ANNUAL_LEAVE.

UPDATE leave_request
SET leave_type_code = 'ANNUAL_LEAVE'
WHERE leave_type_code NOT IN (
    'ANNUAL_LEAVE', 'BIRTH_BAPTISM', 'MARRIAGE_SELF', 'MARRIAGE_KIN',
    'DEATH_CLOSE', 'DEATH_EXTENDED', 'HOSPITALIZATION', 'SICK_LEAVE'
);

UPDATE leaves
SET leave_type_code = 'ANNUAL_LEAVE'
WHERE leave_type_code NOT IN (
    'ANNUAL_LEAVE', 'BIRTH_BAPTISM', 'MARRIAGE_SELF', 'MARRIAGE_KIN',
    'DEATH_CLOSE', 'DEATH_EXTENDED', 'HOSPITALIZATION', 'SICK_LEAVE'
);

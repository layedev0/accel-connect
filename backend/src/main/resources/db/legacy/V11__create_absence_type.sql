-- V11: Absence type reference table

CREATE TABLE absence_type (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50)  NOT NULL UNIQUE
);

-- Seed default absence types
INSERT INTO absence_type (name, code) VALUES
    ('Maladie',              'SICK'),
    ('Quitte tôt',           'EARLY_LEAVE'),
    ('Arrivée tardive',      'LATE_ARRIVAL'),
    ('Long déjeuner',        'LONG_LUNCH'),
    ('Urgence familiale',    'FAMILY_EMERGENCY'),
    ('Rendez-vous',          'APPOINTMENT'),
    ('Rendez-vous médical',  'MEDICAL_APPOINTMENT'),
    ('Télétravail',          'REMOTE_WORK');

-- Add absence_type_id column to absence_request
ALTER TABLE absence_request
    ADD COLUMN absence_type_id BIGINT NULL,
    ADD CONSTRAINT fk_absence_request_type
        FOREIGN KEY (absence_type_id) REFERENCES absence_type(id);

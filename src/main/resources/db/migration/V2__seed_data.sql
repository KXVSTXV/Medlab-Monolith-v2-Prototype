-- ============================================================
--  MedLab v2.0 — Flyway V2 Seed Data
--  Passwords are BCrypt-hashed value of "Admin@123"
--  Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy
-- ============================================================

-- ── Roles ─────────────────────────────────────────────────────

INSERT IGNORE INTO roles (name) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_LAB_MANAGER'),
    ('ROLE_LAB_TECH'),
    ('ROLE_DOCTOR'),
    ('ROLE_RECEPTION'),
    ('ROLE_BILLING');

-- ── Default users ─────────────────────────────────────────────
-- All passwords: Admin@123

INSERT IGNORE INTO users
    (username, email, password_hash, full_name, status, created_by)
VALUES
    ('admin',
     'admin@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'System Administrator', 'ACTIVE', 'SYSTEM'),

    ('labmanager1',
     'labmanager@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Lab Manager One', 'ACTIVE', 'SYSTEM'),

    ('labtech1',
     'labtech1@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Lab Technician One', 'ACTIVE', 'SYSTEM'),

    ('labtech2',
     'labtech2@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Lab Technician Two', 'ACTIVE', 'SYSTEM'),

    ('doctor1',
     'doctor1@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Dr. Priya Sharma', 'ACTIVE', 'SYSTEM'),

    ('reception1',
     'reception1@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Receptionist One', 'ACTIVE', 'SYSTEM'),

    ('billing1',
     'billing1@medlab.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy',
     'Billing Staff One', 'ACTIVE', 'SYSTEM');

-- ── Assign roles to users ────────────────────────────────────

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin'       AND r.name = 'ROLE_ADMIN';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'labmanager1' AND r.name = 'ROLE_LAB_MANAGER';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'labtech1'    AND r.name = 'ROLE_LAB_TECH';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'labtech2'    AND r.name = 'ROLE_LAB_TECH';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'doctor1'     AND r.name = 'ROLE_DOCTOR';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'reception1'  AND r.name = 'ROLE_RECEPTION';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'billing1'    AND r.name = 'ROLE_BILLING';

-- ── Sample patients ───────────────────────────────────────────

INSERT IGNORE INTO patients
    (mrn, full_name, dob, gender, email, mobile_number, blood_group,
     consent_given, status, created_by)
VALUES
    ('MRN-000001','John Doe',       '2000-05-15','M','johndoe@example.com',
     '9876543210','O+', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000002','Jane Smith',     '1995-08-22','F','janesmith@example.com',
     '9876543211','A+', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000003','Richard Roe',    '1988-03-10','M','richardroe@example.com',
     '9876543212','B+', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000004','Emily Johnson',  '2002-11-30','F','emily@example.com',
     '9876543213','AB-',TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000005','Michael Brown',  '1975-07-04','M','michael@example.com',
     '9876543214','O-', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000006','Sarah Wilson',   '1990-12-18','F','sarah@example.com',
     '9876543215','A-', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000007','David Lee',      '1982-04-25','M','davidlee@example.com',
     '9876543216','B-', FALSE,'ACTIVE','SYSTEM'),
    ('MRN-000008','Priya Patel',    '1998-09-14','F','priya@example.com',
     '9876543217','O+', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000009','Ahmed Khan',     '1965-01-07','M','ahmed@example.com',
     '9876543218','A+', TRUE, 'ACTIVE','SYSTEM'),
    ('MRN-000010','Meera Nair',     '1993-06-19','F','meera@example.com',
     '9876543219','AB+',TRUE, 'ACTIVE','SYSTEM');

-- ── Lab test catalog ──────────────────────────────────────────

INSERT IGNORE INTO lab_tests
    (code, name, description, specimen_type, turnaround_hours, price, reference_range,
     active, created_by)
VALUES
    ('CBC',        'Complete Blood Count',
     'Full blood cell count including RBC, WBC, platelets',
     'BLOOD', 4, 250.00, NULL, TRUE, 'SYSTEM'),

    ('CMP',        'Comprehensive Metabolic Panel',
     'Kidney function, liver function, electrolytes and glucose',
     'BLOOD', 6, 450.00, NULL, TRUE, 'SYSTEM'),

    ('LIPID',      'Lipid Profile',
     'Total cholesterol, HDL, LDL, triglycerides',
     'BLOOD', 8, 350.00, NULL, TRUE, 'SYSTEM'),

    ('COVID-PCR',  'COVID-19 RT-PCR',
     'Real-time polymerase chain reaction test for SARS-CoV-2',
     'SWAB', 24, 1200.00, NULL, TRUE, 'SYSTEM'),

    ('URINE-RE',   'Urine Routine & Microscopy',
     'Physical, chemical and microscopic examination of urine',
     'URINE', 2, 150.00, NULL, TRUE, 'SYSTEM'),

    ('THYROID',    'Thyroid Function Test (TSH/T3/T4)',
     'Thyroid stimulating hormone and thyroid hormone levels',
     'BLOOD', 12, 550.00, NULL, TRUE, 'SYSTEM'),

    ('BLOOD-SUGAR','Fasting Blood Sugar',
     'Plasma glucose level after 8-hour fast',
     'BLOOD', 2, 80.00, '70-100 mg/dL', TRUE, 'SYSTEM'),

    ('HBA1C',      'Glycated Haemoglobin (HbA1c)',
     'Average blood glucose level over last 3 months',
     'BLOOD', 6, 300.00, '< 5.7 %', TRUE, 'SYSTEM'),

    ('DENGUE-NS1', 'Dengue NS1 Antigen',
     'Rapid antigen detection for dengue fever',
     'BLOOD', 4, 400.00, 'Negative', TRUE, 'SYSTEM'),

    ('MALARIA',    'Malaria Antigen Test',
     'Rapid test for P.falciparum and P.vivax malaria',
     'BLOOD', 2, 200.00, 'Negative', TRUE, 'SYSTEM');

-- ── Panels ────────────────────────────────────────────────────

INSERT IGNORE INTO panels (code, name, description, active, created_by)
VALUES
    ('BMP',  'Basic Metabolic Panel',  'Core metabolic markers',    TRUE, 'SYSTEM'),
    ('DIAB', 'Diabetes Screening',     'Blood sugar + HbA1c bundle', TRUE, 'SYSTEM');

INSERT IGNORE INTO panel_tests (panel_id, lab_test_id)
SELECT p.id, t.id FROM panels p, lab_tests t
WHERE p.code = 'BMP'
  AND t.code IN ('CMP','BLOOD-SUGAR');

INSERT IGNORE INTO panel_tests (panel_id, lab_test_id)
SELECT p.id, t.id FROM panels p, lab_tests t
WHERE p.code = 'DIAB'
  AND t.code IN ('BLOOD-SUGAR','HBA1C');

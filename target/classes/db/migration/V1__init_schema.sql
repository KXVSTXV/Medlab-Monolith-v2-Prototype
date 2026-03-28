-- ============================================================
--  MedLab v2.0 — Flyway V1 Schema
--  MySQL 8.x  |  All tables include audit fields & soft-delete
-- ============================================================

-- Roles (reference data — no soft-delete, no audit fields)
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50)  NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Users
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150),
    status        ENUM('ACTIVE','INACTIVE','LOCKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User ↔ Role join table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patients
CREATE TABLE IF NOT EXISTS patients (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    mrn                   VARCHAR(50)  UNIQUE,
    full_name             VARCHAR(150) NOT NULL,
    dob                   DATE,
    gender                ENUM('M','F','O') NOT NULL,
    email                 VARCHAR(100) UNIQUE,
    mobile_number         VARCHAR(15)  UNIQUE,
    address               VARCHAR(300),
    blood_group           VARCHAR(10),
    allergies             TEXT,
    medical_history       TEXT,
    consent_given         BOOLEAN      NOT NULL DEFAULT FALSE,
    status                ENUM('ACTIVE','INACTIVE','DISCHARGED') NOT NULL DEFAULT 'ACTIVE',
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                       ON UPDATE CURRENT_TIMESTAMP(6),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Lab Tests (catalog)
CREATE TABLE IF NOT EXISTS lab_tests (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50)   NOT NULL UNIQUE,
    name             VARCHAR(255)  NOT NULL,
    description      TEXT,
    specimen_type    VARCHAR(50),
    turnaround_hours INT,
    price            DECIMAL(12,2),
    reference_range  VARCHAR(200),
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                   ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Panels (groups of lab tests)
CREATE TABLE IF NOT EXISTS panels (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                             ON UPDATE CURRENT_TIMESTAMP(6),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Panel ↔ LabTest join table
CREATE TABLE IF NOT EXISTS panel_tests (
    panel_id    BIGINT NOT NULL,
    lab_test_id BIGINT NOT NULL,
    PRIMARY KEY (panel_id, lab_test_id),
    CONSTRAINT fk_pt_panel FOREIGN KEY (panel_id)    REFERENCES panels(id),
    CONSTRAINT fk_pt_test  FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Appointments
CREATE TABLE IF NOT EXISTS appointments (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    patient_id   BIGINT       NOT NULL,
    scheduled_at DATETIME     NOT NULL,
    collector_id BIGINT,
    location     VARCHAR(255),
    notes        TEXT,
    status       ENUM('SCHEDULED','CONFIRMED','SAMPLE_COLLECTED','COMPLETED','CANCELLED')
                              NOT NULL DEFAULT 'SCHEDULED',
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                              ON UPDATE CURRENT_TIMESTAMP(6),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Samples
CREATE TABLE IF NOT EXISTS samples (
    id               BIGINT      AUTO_INCREMENT PRIMARY KEY,
    appointment_id   BIGINT      NOT NULL,
    barcode          VARCHAR(100) UNIQUE,
    specimen_type    ENUM('BLOOD','URINE','STOOL','SWAB','SPUTUM','CSF','OTHER') NOT NULL,
    collected_at     DATETIME,
    status           ENUM('COLLECTED','PROCESSING','ANALYSED','REJECTED')
                                 NOT NULL DEFAULT 'COLLECTED',
    rejection_reason VARCHAR(255),
    collected_by     VARCHAR(100),
    is_deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    CONSTRAINT fk_sample_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Orders
CREATE TABLE IF NOT EXISTS test_orders (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    sample_id   BIGINT      NOT NULL,
    lab_test_id BIGINT      NOT NULL,
    ordered_by  VARCHAR(100),
    status      ENUM('ORDERED','SAMPLE_COLLECTED','IN_PROGRESS','RESULT_ENTERED',
                     'VERIFIED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ORDERED',
    priority    ENUM('ROUTINE','URGENT','STAT')           NOT NULL DEFAULT 'ROUTINE',
    notes       TEXT,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                            ON UPDATE CURRENT_TIMESTAMP(6),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    CONSTRAINT fk_to_sample  FOREIGN KEY (sample_id)   REFERENCES samples(id),
    CONSTRAINT fk_to_labtest FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Test Results
CREATE TABLE IF NOT EXISTS test_results (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    test_order_id   BIGINT       NOT NULL UNIQUE,
    value           VARCHAR(255),
    unit            VARCHAR(50),
    reference_range VARCHAR(200),
    is_abnormal     BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_by     VARCHAR(100),
    verified_at     DATETIME,
    status          ENUM('PENDING','ENTERED','VERIFIED') NOT NULL DEFAULT 'PENDING',
    remarks         TEXT,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT fk_tr_order FOREIGN KEY (test_order_id) REFERENCES test_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reports
CREATE TABLE IF NOT EXISTS reports (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    report_number  VARCHAR(100) NOT NULL UNIQUE,
    patient_id     BIGINT       NOT NULL,
    appointment_id BIGINT,
    generated_at   DATETIME,
    pdf_ref        VARCHAR(500),
    has_abnormal   BOOLEAN      NOT NULL DEFAULT FALSE,
    prepared_by    VARCHAR(100),
    verified_by    VARCHAR(100),
    status         ENUM('DRAFT','VERIFIED','RELEASED') NOT NULL DEFAULT 'DRAFT',
    summary        TEXT,
    is_deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                ON UPDATE CURRENT_TIMESTAMP(6),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT fk_rpt_patient FOREIGN KEY (patient_id)     REFERENCES patients(id),
    CONSTRAINT fk_rpt_appt   FOREIGN KEY (appointment_id)  REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Invoices
CREATE TABLE IF NOT EXISTS invoices (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(100)  NOT NULL UNIQUE,
    patient_id     BIGINT        NOT NULL,
    appointment_id BIGINT,
    amount         DECIMAL(12,2) NOT NULL,
    tax_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    due_date       DATE,
    status         ENUM('PENDING','PAID','OVERDUE','CANCELLED','PARTIALLY_PAID')
                                  NOT NULL DEFAULT 'PENDING',
    line_items     TEXT,
    is_deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    CONSTRAINT fk_inv_patient FOREIGN KEY (patient_id)    REFERENCES patients(id),
    CONSTRAINT fk_inv_appt   FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    invoice_id   BIGINT        NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    method       ENUM('CASH','CARD','UPI','NET_BANKING','CHEQUE') NOT NULL DEFAULT 'CASH',
    provider_ref VARCHAR(255),
    status       ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    processed_at DATETIME,
    remarks      VARCHAR(255),
    is_deleted   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                               ON UPDATE CURRENT_TIMESTAMP(6),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    CONSTRAINT fk_pay_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notification Logs
CREATE TABLE IF NOT EXISTS notification_logs (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT,
    user_id         BIGINT,
    type            ENUM('ORDER_CREATED','SAMPLE_COLLECTED','RESULT_ENTERED','REPORT_READY',
                         'INVOICE_CREATED','PAYMENT_SUCCESS','APPOINTMENT_SCHEDULED',
                         'ORDER_CANCELLED','GENERAL') NOT NULL,
    channel         ENUM('SYSTEM','EMAIL','SMS') NOT NULL DEFAULT 'SYSTEM',
    message         TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    delivery_status ENUM('SENT','FAILED','PENDING') NOT NULL DEFAULT 'SENT',
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                ON UPDATE CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit Logs (immutable — no soft-delete, no updated_at)
CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    actor      VARCHAR(100) NOT NULL,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(100) NOT NULL,
    entity_id  BIGINT,
    details    TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_actor  (actor),
    INDEX idx_audit_entity (entity),
    INDEX idx_audit_ts     (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Useful indexes
CREATE INDEX idx_patients_mrn     ON patients(mrn);
CREATE INDEX idx_patients_mobile  ON patients(mobile_number);
CREATE INDEX idx_samples_barcode  ON samples(barcode);
CREATE INDEX idx_appt_patient     ON appointments(patient_id);
CREATE INDEX idx_appt_status      ON appointments(status);
CREATE INDEX idx_to_status        ON test_orders(status);
CREATE INDEX idx_rpt_patient      ON reports(patient_id);
CREATE INDEX idx_inv_patient      ON invoices(patient_id);
CREATE INDEX idx_notif_patient    ON notification_logs(patient_id);

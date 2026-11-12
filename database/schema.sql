-- =====================================================================
-- CyberShield - REFERENCE SCHEMA SCRIPT (generated from the Flyway migrations)
--
-- The AUTHORITATIVE schema is src/main/resources/db/migration (Flyway runs it automatically
-- when the application starts). This file is the same SQL concatenated in order, for reading,
-- printing in the project report or exporting to MySQL Workbench.
--
-- DO NOT run this file on a database that Flyway has already migrated (e.g. cybershield_db
-- after the application has started once) - every CREATE TABLE would fail.
-- To try it standalone, use a NEW empty database:
--     CREATE DATABASE cybershield_reference;  USE cybershield_reference;  SOURCE schema.sql;
-- =====================================================================


-- ---------------------------------------------------------------------
-- V1__create_reference_and_user_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V1: Reference tables (roles, departments, office locations, frameworks) and users
-- Every table uses a BIGINT surrogate key plus human-readable business codes.
-- =====================================================================

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT chk_roles_code CHECK (code IN ('SECURITY_ANALYST', 'IT_INFRASTRUCTURE_ENGINEER',
                                              'COMPLIANCE_AUDITOR', 'SECURITY_MANAGER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE office_locations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(20)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    country    VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_office_locations_code UNIQUE (code),
    CONSTRAINT uk_office_locations_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE compliance_frameworks (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    version_label VARCHAR(50)  NULL,
    description   TEXT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_compliance_frameworks_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(150) NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_id       BIGINT       NOT NULL,
    department_id BIGINT       NULL,
    job_title     VARCHAR(100) NULL,
    phone         VARCHAR(30)  NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- BCrypt hashes are always 60 characters and start with $2
    CONSTRAINT chk_users_password_hash CHECK (CHAR_LENGTH(password_hash) = 60 AND password_hash LIKE '$2%')
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V2__create_asset_and_software_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V2: IT assets, the software catalogue and the asset_software junction table
-- =====================================================================

CREATE TABLE assets (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    asset_code       VARCHAR(30)  NOT NULL,
    name             VARCHAR(150) NOT NULL,
    asset_type       VARCHAR(30)  NOT NULL,
    hostname         VARCHAR(255) NULL,
    ip_address       VARCHAR(45)  NULL,
    operating_system VARCHAR(100) NULL,
    department_id    BIGINT       NOT NULL,
    location_id      BIGINT       NOT NULL,
    owner_id         BIGINT       NULL,
    criticality      VARCHAR(20)  NOT NULL,
    environment      VARCHAR(30)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    description      TEXT         NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assets_asset_code UNIQUE (asset_code),
    CONSTRAINT fk_assets_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_assets_location FOREIGN KEY (location_id) REFERENCES office_locations (id),
    CONSTRAINT fk_assets_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT chk_assets_type CHECK (asset_type IN ('SERVER', 'WORKSTATION', 'LAPTOP', 'NETWORK_DEVICE', 'DATABASE',
                                                     'CLOUD_INSTANCE', 'APPLICATION', 'MOBILE_DEVICE')),
    CONSTRAINT chk_assets_criticality CHECK (criticality IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_assets_environment CHECK (environment IN ('PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TEST',
                                                             'DISASTER_RECOVERY')),
    CONSTRAINT chk_assets_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'INACTIVE', 'ARCHIVED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE software (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    name             VARCHAR(150) NOT NULL,
    vendor           VARCHAR(150) NOT NULL,
    category         VARCHAR(30)  NOT NULL,
    baseline_version VARCHAR(50)  NULL COMMENT 'Company-approved minimum version; lower installed versions are outdated',
    description      TEXT         NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_software_name_vendor UNIQUE (name, vendor),
    CONSTRAINT chk_software_category CHECK (category IN ('OPERATING_SYSTEM', 'DATABASE', 'WEB_SERVER',
                                                         'APPLICATION_SERVER', 'SECURITY', 'PRODUCTIVITY',
                                                         'DEVELOPMENT', 'NETWORKING', 'BROWSER', 'OTHER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Many-to-many: which software is installed on which asset.
-- The installed version lives HERE because two assets can run different versions of the same product.
CREATE TABLE asset_software (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    asset_id            BIGINT      NOT NULL,
    software_id         BIGINT      NOT NULL,
    installed_version   VARCHAR(50) NOT NULL,
    installation_date   DATE        NULL,
    license_type        VARCHAR(30) NOT NULL,
    license_expiry_date DATE        NULL,
    created_at          DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_software UNIQUE (asset_id, software_id),
    CONSTRAINT fk_asset_software_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_asset_software_software FOREIGN KEY (software_id) REFERENCES software (id),
    CONSTRAINT chk_asset_software_license CHECK (license_type IN ('OPEN_SOURCE', 'COMMERCIAL', 'SUBSCRIPTION',
                                                                  'FREEWARE', 'ENTERPRISE')),
    CONSTRAINT chk_asset_software_dates CHECK (license_expiry_date IS NULL OR installation_date IS NULL
                                               OR license_expiry_date >= installation_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V3__create_vulnerability_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V3: Vulnerability definitions and asset_vulnerabilities (one vulnerability on one asset)
-- =====================================================================

CREATE TABLE vulnerabilities (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    vuln_code               VARCHAR(50)  NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    description             TEXT         NOT NULL,
    cve_id                  VARCHAR(30)  NULL,
    severity                VARCHAR(20)  NOT NULL,
    cvss_score              DECIMAL(3, 1) NOT NULL,
    recommended_remediation TEXT         NULL,
    created_by              BIGINT       NULL,
    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vulnerabilities_code UNIQUE (vuln_code),
    CONSTRAINT uk_vulnerabilities_cve UNIQUE (cve_id),
    CONSTRAINT fk_vulnerabilities_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_vulnerabilities_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_vulnerabilities_cvss CHECK (cvss_score BETWEEN 0.0 AND 10.0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Junction table and an entity in its own right: "vulnerability X found on asset Y".
-- Remediation tasks and risk assessments reference a row of this table, so there is never
-- any doubt about WHICH server a task is fixing.
CREATE TABLE asset_vulnerabilities (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    asset_id           BIGINT      NOT NULL,
    vulnerability_id   BIGINT      NOT NULL,
    discovered_date    DATE        NOT NULL,
    last_observed_date DATE        NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes              TEXT        NULL,
    created_by         BIGINT      NULL,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_vulnerability UNIQUE (asset_id, vulnerability_id),
    CONSTRAINT fk_asset_vuln_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_asset_vuln_vulnerability FOREIGN KEY (vulnerability_id) REFERENCES vulnerabilities (id),
    CONSTRAINT fk_asset_vuln_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_asset_vuln_status CHECK (status IN ('OPEN', 'MITIGATING', 'MITIGATED', 'ACCEPTED', 'CLOSED')),
    CONSTRAINT chk_asset_vuln_dates CHECK (last_observed_date IS NULL OR last_observed_date >= discovered_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V4__create_remediation_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V4: Remediation workflow - tasks, append-only history, evidence and evidence reviews
-- =====================================================================

CREATE TABLE remediation_tasks (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    task_code              VARCHAR(50)  NOT NULL,
    asset_vulnerability_id BIGINT       NOT NULL,
    title                  VARCHAR(200) NOT NULL,
    description            TEXT         NOT NULL,
    assigned_engineer_id   BIGINT       NULL,
    created_by             BIGINT       NOT NULL,
    due_date               DATE         NULL,
    priority               VARCHAR(20)  NOT NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    completed_at           DATETIME     NULL,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_remediation_tasks_code UNIQUE (task_code),
    CONSTRAINT fk_task_asset_vulnerability FOREIGN KEY (asset_vulnerability_id) REFERENCES asset_vulnerabilities (id),
    CONSTRAINT fk_task_engineer FOREIGN KEY (assigned_engineer_id) REFERENCES users (id),
    CONSTRAINT fk_task_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_task_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_VERIFICATION',
                                                 'COMPLETED', 'REOPENED')),
    -- Any status after OPEN requires an engineer.
    CONSTRAINT chk_task_assignee CHECK (status = 'OPEN' OR assigned_engineer_id IS NOT NULL),
    -- completed_at is set exactly when the task is COMPLETED.
    CONSTRAINT chk_task_completed_at CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Append-only: the application only ever INSERTs into this table.
CREATE TABLE remediation_history (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    task_id              BIGINT      NOT NULL,
    action_type          VARCHAR(30) NOT NULL,
    previous_status      VARCHAR(30) NULL,
    new_status           VARCHAR(30) NULL,
    previous_assignee_id BIGINT      NULL,
    new_assignee_id      BIGINT      NULL,
    performed_by_id      BIGINT      NOT NULL,
    description          TEXT        NOT NULL,
    created_at           DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_task FOREIGN KEY (task_id) REFERENCES remediation_tasks (id),
    CONSTRAINT fk_history_prev_assignee FOREIGN KEY (previous_assignee_id) REFERENCES users (id),
    CONSTRAINT fk_history_new_assignee FOREIGN KEY (new_assignee_id) REFERENCES users (id),
    CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by_id) REFERENCES users (id),
    CONSTRAINT chk_history_action CHECK (action_type IN ('CREATED', 'ASSIGNED', 'REASSIGNED', 'STATUS_CHANGED',
                                                         'NOTE_ADDED', 'EVIDENCE_UPLOADED', 'EVIDENCE_SUBMITTED',
                                                         'EVIDENCE_APPROVED', 'EVIDENCE_REJECTED', 'REOPENED',
                                                         'UPDATED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE remediation_evidence (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    task_id           BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(100) NOT NULL,
    file_path         VARCHAR(500) NOT NULL COMMENT 'Path relative to the configured upload directory',
    mime_type         VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    description       TEXT         NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_by_id   BIGINT       NOT NULL,
    submitted_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_remediation_evidence_stored UNIQUE (stored_filename),
    CONSTRAINT fk_evidence_task FOREIGN KEY (task_id) REFERENCES remediation_tasks (id),
    CONSTRAINT fk_evidence_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users (id),
    CONSTRAINT chk_evidence_mime CHECK (mime_type IN ('application/pdf', 'image/png', 'image/jpeg', 'text/plain')),
    CONSTRAINT chk_evidence_size CHECK (file_size > 0 AND file_size <= 10485760),
    CONSTRAINT chk_evidence_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Every review decision is preserved (a task can be rejected several times before approval).
CREATE TABLE remediation_evidence_reviews (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    evidence_id BIGINT      NOT NULL,
    reviewer_id BIGINT      NOT NULL,
    decision    VARCHAR(20) NOT NULL,
    comments    TEXT        NULL,
    reviewed_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evidence_review_evidence FOREIGN KEY (evidence_id) REFERENCES remediation_evidence (id),
    CONSTRAINT fk_evidence_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT chk_evidence_review_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V5__create_alert_and_incident_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V5: Security alerts (recorded in MySQL - no live SIEM integration) and incident management
-- =====================================================================

CREATE TABLE security_alerts (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    alert_code          VARCHAR(50)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT         NULL,
    source              VARCHAR(30)  NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    asset_id            BIGINT       NULL,
    detected_at         DATETIME     NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    assigned_analyst_id BIGINT       NULL,
    created_by          BIGINT       NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_security_alerts_code UNIQUE (alert_code),
    CONSTRAINT fk_alert_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_alert_analyst FOREIGN KEY (assigned_analyst_id) REFERENCES users (id),
    CONSTRAINT fk_alert_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_alert_source CHECK (source IN ('IDS', 'EDR', 'FIREWALL', 'ANTIVIRUS', 'EMAIL_GATEWAY',
                                                  'CLOUD_MONITOR', 'DLP', 'MANUAL')),
    CONSTRAINT chk_alert_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (status IN ('NEW', 'INVESTIGATING', 'ESCALATED', 'RESOLVED', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE incidents (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    incident_code       VARCHAR(50)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT         NOT NULL,
    incident_type       VARCHAR(30)  NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_analyst_id BIGINT       NULL,
    detected_at         DATETIME     NOT NULL,
    resolution_details  TEXT         NULL,
    resolved_at         DATETIME     NULL,
    created_by          BIGINT       NOT NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_incidents_code UNIQUE (incident_code),
    CONSTRAINT fk_incident_analyst FOREIGN KEY (assigned_analyst_id) REFERENCES users (id),
    CONSTRAINT fk_incident_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_incident_type CHECK (incident_type IN ('MALWARE', 'PHISHING', 'UNAUTHORIZED_ACCESS',
                                                          'DATA_BREACH', 'DENIAL_OF_SERVICE', 'INSIDER_THREAT',
                                                          'POLICY_VIOLATION', 'RECONNAISSANCE', 'OTHER')),
    CONSTRAINT chk_incident_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incident_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED', 'CLOSED')),
    -- A resolved or closed incident must say how it was resolved.
    CONSTRAINT chk_incident_resolution CHECK (status NOT IN ('RESOLVED', 'CLOSED') OR resolution_details IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Many-to-many: incidents <-> alerts
CREATE TABLE incident_alerts (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    incident_id  BIGINT   NOT NULL,
    alert_id     BIGINT   NOT NULL,
    linked_by_id BIGINT   NOT NULL,
    linked_at    DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_incident_alert UNIQUE (incident_id, alert_id),
    CONSTRAINT fk_incident_alerts_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_incident_alerts_alert FOREIGN KEY (alert_id) REFERENCES security_alerts (id),
    CONSTRAINT fk_incident_alerts_linked_by FOREIGN KEY (linked_by_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Many-to-many: incidents <-> affected assets
CREATE TABLE incident_assets (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    incident_id        BIGINT       NOT NULL,
    asset_id           BIGINT       NOT NULL,
    impact_description VARCHAR(500) NULL,
    linked_by_id       BIGINT       NOT NULL,
    linked_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_incident_asset UNIQUE (incident_id, asset_id),
    CONSTRAINT fk_incident_assets_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_incident_assets_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_incident_assets_linked_by FOREIGN KEY (linked_by_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE investigation_notes (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    incident_id BIGINT   NOT NULL,
    author_id   BIGINT   NOT NULL,
    note        TEXT     NOT NULL,
    created_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notes_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_notes_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V6__create_audit_and_compliance_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V6: Compliance controls, audit plans, findings, corrective actions, audit evidence and reviews
-- (Audit evidence is deliberately separate from remediation evidence.)
-- =====================================================================

CREATE TABLE compliance_controls (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    framework_id BIGINT       NOT NULL,
    control_code VARCHAR(30)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NULL,
    category     VARCHAR(100) NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_control_framework_code UNIQUE (framework_id, control_code),
    CONSTRAINT fk_control_framework FOREIGN KEY (framework_id) REFERENCES compliance_frameworks (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_plans (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    plan_code              VARCHAR(50)  NOT NULL,
    name                   VARCHAR(200) NOT NULL,
    framework_id           BIGINT       NOT NULL,
    description            TEXT         NULL,
    assigned_auditor_id    BIGINT       NOT NULL,
    start_date             DATE         NOT NULL,
    target_completion_date DATE         NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_by             BIGINT       NOT NULL,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_audit_plans_code UNIQUE (plan_code),
    CONSTRAINT fk_audit_plan_framework FOREIGN KEY (framework_id) REFERENCES compliance_frameworks (id),
    CONSTRAINT fk_audit_plan_auditor FOREIGN KEY (assigned_auditor_id) REFERENCES users (id),
    CONSTRAINT fk_audit_plan_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_audit_plan_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_audit_plan_dates CHECK (target_completion_date >= start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_findings (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    finding_code  VARCHAR(50)  NOT NULL,
    audit_plan_id BIGINT       NOT NULL,
    control_id    BIGINT       NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT         NOT NULL,
    severity      VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    due_date      DATE         NULL,
    created_by    BIGINT       NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_audit_findings_code UNIQUE (finding_code),
    CONSTRAINT fk_finding_plan FOREIGN KEY (audit_plan_id) REFERENCES audit_plans (id),
    CONSTRAINT fk_finding_control FOREIGN KEY (control_id) REFERENCES compliance_controls (id),
    CONSTRAINT fk_finding_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_finding_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_finding_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- One finding can need several corrective actions (one-to-many).
CREATE TABLE corrective_actions (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    finding_id     BIGINT      NOT NULL,
    assigned_to_id BIGINT      NOT NULL,
    description    TEXT        NOT NULL,
    deadline       DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by     BIGINT      NOT NULL,
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_corrective_finding FOREIGN KEY (finding_id) REFERENCES audit_findings (id),
    CONSTRAINT fk_corrective_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id),
    CONSTRAINT fk_corrective_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_corrective_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING_REVIEW', 'VERIFIED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_evidence (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    corrective_action_id BIGINT       NOT NULL,
    original_filename    VARCHAR(255) NOT NULL,
    stored_filename      VARCHAR(100) NOT NULL,
    file_path            VARCHAR(500) NOT NULL COMMENT 'Path relative to the configured upload directory',
    mime_type            VARCHAR(100) NOT NULL,
    file_size            BIGINT       NOT NULL,
    description          TEXT         NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    submitted_by_id      BIGINT       NOT NULL,
    submitted_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_audit_evidence_stored UNIQUE (stored_filename),
    CONSTRAINT fk_audit_evidence_action FOREIGN KEY (corrective_action_id) REFERENCES corrective_actions (id),
    CONSTRAINT fk_audit_evidence_submitted_by FOREIGN KEY (submitted_by_id) REFERENCES users (id),
    CONSTRAINT chk_audit_evidence_mime CHECK (mime_type IN ('application/pdf', 'image/png', 'image/jpeg', 'text/plain')),
    CONSTRAINT chk_audit_evidence_size CHECK (file_size > 0 AND file_size <= 10485760),
    CONSTRAINT chk_audit_evidence_status CHECK (status IN ('SUBMITTED', 'VERIFIED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_evidence_reviews (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    evidence_id  BIGINT      NOT NULL,
    reviewer_id  BIGINT      NOT NULL,
    decision     VARCHAR(20) NOT NULL,
    review_notes TEXT        NULL,
    reviewed_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_review_evidence FOREIGN KEY (evidence_id) REFERENCES audit_evidence (id),
    CONSTRAINT fk_audit_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT chk_audit_review_decision CHECK (decision IN ('VERIFIED', 'REJECTED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V7__create_risk_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V7: Risk assessments (score = likelihood x impact) and risk treatments
-- The CHECK constraints guarantee the stored score and level always match the formula,
-- even if someone bypasses the application.
-- =====================================================================

CREATE TABLE risk_assessments (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    risk_code              VARCHAR(50)  NOT NULL,
    title                  VARCHAR(200) NOT NULL,
    description            TEXT         NULL,
    asset_id               BIGINT       NOT NULL,
    asset_vulnerability_id BIGINT       NULL,
    likelihood             INT          NOT NULL,
    impact                 INT          NOT NULL,
    risk_score             INT          NOT NULL,
    risk_level             VARCHAR(20)  NOT NULL,
    treatment_strategy     VARCHAR(20)  NOT NULL,
    owner_id               BIGINT       NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    created_by             BIGINT       NOT NULL,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_risk_assessments_code UNIQUE (risk_code),
    CONSTRAINT fk_risk_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_risk_asset_vulnerability FOREIGN KEY (asset_vulnerability_id) REFERENCES asset_vulnerabilities (id),
    CONSTRAINT fk_risk_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_risk_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_risk_likelihood CHECK (likelihood BETWEEN 1 AND 5),
    CONSTRAINT chk_risk_impact CHECK (impact BETWEEN 1 AND 5),
    CONSTRAINT chk_risk_score CHECK (risk_score = likelihood * impact),
    CONSTRAINT chk_risk_level CHECK (
        (risk_score BETWEEN 1 AND 4 AND risk_level = 'LOW') OR
        (risk_score BETWEEN 5 AND 9 AND risk_level = 'MEDIUM') OR
        (risk_score BETWEEN 10 AND 16 AND risk_level = 'HIGH') OR
        (risk_score BETWEEN 17 AND 25 AND risk_level = 'CRITICAL')),
    CONSTRAINT chk_risk_strategy CHECK (treatment_strategy IN ('MITIGATE', 'AVOID', 'TRANSFER', 'ACCEPT')),
    CONSTRAINT chk_risk_status CHECK (status IN ('OPEN', 'TREATMENT_PLANNED', 'IN_TREATMENT', 'MONITORING', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE risk_treatments (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    risk_id            BIGINT      NOT NULL,
    strategy           VARCHAR(20) NOT NULL,
    action_description TEXT        NOT NULL,
    owner_id           BIGINT      NULL,
    deadline           DATE        NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_by         BIGINT      NOT NULL,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_treatment_risk FOREIGN KEY (risk_id) REFERENCES risk_assessments (id),
    CONSTRAINT fk_treatment_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_treatment_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_treatment_strategy CHECK (strategy IN ('MITIGATE', 'AVOID', 'TRANSFER', 'ACCEPT')),
    CONSTRAINT chk_treatment_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V8__create_notification_and_audit_log_tables.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V8: Per-user notifications and the append-only audit log
-- =====================================================================

CREATE TABLE notifications (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT        NOT NULL,
    category     VARCHAR(40)   NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    record_type  VARCHAR(40)   NULL,
    record_id    BIGINT        NULL,
    dedup_key    VARCHAR(150)  NULL,
    created_at   DATETIME      NOT NULL,
    read_at      DATETIME      NULL,
    PRIMARY KEY (id),
    -- Prevents duplicate reminders (e.g. the scheduler running twice). NULL keys are not deduplicated.
    CONSTRAINT uk_notifications_dedup UNIQUE (recipient_id, dedup_key),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT chk_notifications_category CHECK (category IN (
        'CRITICAL_VULNERABILITY', 'ALERT_ASSIGNED', 'INCIDENT_ASSIGNED', 'TASK_ASSIGNED', 'TASK_DEADLINE',
        'TASK_OVERDUE', 'EVIDENCE_SUBMITTED', 'EVIDENCE_APPROVED', 'EVIDENCE_REJECTED', 'TASK_COMPLETED',
        'TASK_REOPENED', 'AUDIT_FINDING', 'CORRECTIVE_ACTION_ASSIGNED', 'AUDIT_EVIDENCE_SUBMITTED',
        'AUDIT_EVIDENCE_REVIEWED', 'SYSTEM'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Append-only application activity log. user_id is NULL for failed logins with an unknown username.
CREATE TABLE audit_logs (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NULL,
    username    VARCHAR(100)  NULL COMMENT 'Actor username, or the attempted username for failed logins',
    action      VARCHAR(40)   NOT NULL,
    module      VARCHAR(40)   NOT NULL,
    record_type VARCHAR(40)   NULL,
    record_id   BIGINT        NULL,
    description VARCHAR(1000) NOT NULL,
    ip_address  VARCHAR(45)   NULL,
    created_at  DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;


-- ---------------------------------------------------------------------
-- V9__add_indexes_and_views.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V9: Secondary indexes for frequently searched / filtered columns, plus reporting views.
-- (Foreign key columns are indexed automatically by InnoDB.)
-- =====================================================================

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_full_name ON users (full_name);

CREATE INDEX idx_assets_name ON assets (name);
CREATE INDEX idx_assets_status ON assets (status);
CREATE INDEX idx_assets_type ON assets (asset_type);
CREATE INDEX idx_assets_environment ON assets (environment);
CREATE INDEX idx_assets_ip ON assets (ip_address);
CREATE INDEX idx_assets_hostname ON assets (hostname);

CREATE INDEX idx_software_name ON software (name);

CREATE INDEX idx_vulnerabilities_severity ON vulnerabilities (severity);
CREATE INDEX idx_vulnerabilities_title ON vulnerabilities (title);
CREATE INDEX idx_asset_vuln_status ON asset_vulnerabilities (status);

CREATE INDEX idx_tasks_status ON remediation_tasks (status);
CREATE INDEX idx_tasks_due_date ON remediation_tasks (due_date);
CREATE INDEX idx_tasks_engineer_status ON remediation_tasks (assigned_engineer_id, status);
CREATE INDEX idx_history_task_created ON remediation_history (task_id, created_at);
CREATE INDEX idx_evidence_task_status ON remediation_evidence (task_id, status);

CREATE INDEX idx_alerts_status ON security_alerts (status);
CREATE INDEX idx_alerts_severity ON security_alerts (severity);
CREATE INDEX idx_alerts_detected_at ON security_alerts (detected_at);
CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_detected_at ON incidents (detected_at);

CREATE INDEX idx_audit_plans_status ON audit_plans (status);
CREATE INDEX idx_findings_status ON audit_findings (status);
CREATE INDEX idx_corrective_status ON corrective_actions (status);

CREATE INDEX idx_risks_level ON risk_assessments (risk_level);
CREATE INDEX idx_risks_status ON risk_assessments (status);

CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, read_at);
CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_id, created_at);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_record ON audit_logs (record_type, record_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_module ON audit_logs (module);

-- ---------------------------------------------------------------------
-- Reporting view: vulnerability exposure per asset (JOIN + GROUP BY + conditional aggregation).
-- Used by the asset inventory report.
-- ---------------------------------------------------------------------
CREATE VIEW v_asset_vulnerability_summary AS
SELECT a.id                                                                      AS asset_id,
       a.asset_code                                                              AS asset_code,
       a.name                                                                    AS asset_name,
       a.status                                                                  AS asset_status,
       COUNT(av.id)                                                              AS total_findings,
       COALESCE(SUM(CASE WHEN av.status IN ('OPEN', 'MITIGATING') THEN 1 ELSE 0 END), 0) AS open_findings,
       COALESCE(SUM(CASE WHEN av.status IN ('OPEN', 'MITIGATING') AND v.severity = 'CRITICAL'
                         THEN 1 ELSE 0 END), 0)                                  AS open_critical_findings,
       MAX(v.cvss_score)                                                         AS highest_cvss
FROM assets a
         LEFT JOIN asset_vulnerabilities av ON av.asset_id = a.id
         LEFT JOIN vulnerabilities v ON v.id = av.vulnerability_id
GROUP BY a.id, a.asset_code, a.name, a.status;

-- ---------------------------------------------------------------------
-- Reporting view: remediation tasks with their asset and vulnerability (multi-table JOIN).
-- ---------------------------------------------------------------------
CREATE VIEW v_remediation_overview AS
SELECT t.id          AS task_id,
       t.task_code   AS task_code,
       t.title       AS task_title,
       t.status      AS task_status,
       t.priority    AS priority,
       t.due_date    AS due_date,
       a.asset_code  AS asset_code,
       a.name        AS asset_name,
       v.vuln_code   AS vuln_code,
       v.title       AS vulnerability_title,
       v.severity    AS severity,
       av.status     AS vulnerability_status,
       u.full_name   AS engineer_name
FROM remediation_tasks t
         JOIN asset_vulnerabilities av ON av.id = t.asset_vulnerability_id
         JOIN assets a ON a.id = av.asset_id
         JOIN vulnerabilities v ON v.id = av.vulnerability_id
         LEFT JOIN users u ON u.id = t.assigned_engineer_id;


-- ---------------------------------------------------------------------
-- V10__seed_reference_data.sql
-- ---------------------------------------------------------------------
-- =====================================================================
-- V10: Reference data required in EVERY environment (including production):
-- roles, departments, office locations, compliance frameworks and controls.
-- Demo users and sample records are NOT created here - see DemoDataInitializer (dev profile only).
-- Framework/control texts are short summaries written for this demonstration; CyberShield does not
-- claim certification against ISO/IEC 27001 or the NIST CSF.
-- =====================================================================

INSERT INTO roles (code, name, description) VALUES
('SECURITY_ANALYST', 'Security Analyst',
 'Manages vulnerabilities, alerts, incidents and risk; creates and reviews remediation tasks.'),
('IT_INFRASTRUCTURE_ENGINEER', 'IT Infrastructure Engineer',
 'Works on remediation tasks assigned to them and submits completion evidence.'),
('COMPLIANCE_AUDITOR', 'Compliance Auditor',
 'Plans audits, records findings, assigns corrective actions and verifies audit evidence.'),
('SECURITY_MANAGER', 'Security Manager',
 'Highest permission level: users, oversight of every module, approvals, reports and system health.');

INSERT INTO departments (code, name, description) VALUES
('FIN', 'Finance', 'Accounting, payroll and financial reporting'),
('HR', 'Human Resources', 'People operations and HR systems'),
('IT', 'Information Technology', 'Corporate IT, networking and end-user computing'),
('ENG', 'Engineering', 'Product and platform engineering teams'),
('SAL', 'Sales and Marketing', 'Sales, marketing and customer success'),
('OPS', 'Operations', 'Business operations, data and analytics'),
('SEC', 'Information Security', 'Security operations and governance'),
('LEG', 'Legal and Compliance', 'Legal, risk and regulatory compliance');

INSERT INTO office_locations (code, name, city, country, address) VALUES
('HYD', 'Hyderabad Headquarters', 'Hyderabad', 'India', 'HITEC City, Hyderabad, Telangana'),
('BLR', 'Bengaluru Technology Centre', 'Bengaluru', 'India', 'Whitefield, Bengaluru, Karnataka'),
('PUN', 'Pune Delivery Centre', 'Pune', 'India', 'Hinjewadi, Pune, Maharashtra');

INSERT INTO compliance_frameworks (code, name, version_label, description) VALUES
('ISO27001', 'ISO/IEC 27001 (demonstration subset)', '2022',
 'A small subset of Annex A information security controls used to demonstrate audit planning. Control names are summarised for this project.'),
('NIST_CSF', 'NIST Cybersecurity Framework (demonstration subset)', '2.0',
 'A small subset of NIST CSF outcomes used to demonstrate compliance tracking. Outcome texts are summarised for this project.');

INSERT INTO compliance_controls (framework_id, control_code, title, description, category, created_at)
SELECT f.id, c.control_code, c.title, c.description, c.category, NOW()
FROM compliance_frameworks f
JOIN (
    SELECT 'ISO27001' AS fw, 'A.5.1' AS control_code, 'Information security policies' AS title,
           'Security policies are defined, approved by management, published and reviewed.' AS description,
           'Organizational controls' AS category
    UNION ALL SELECT 'ISO27001', 'A.5.15', 'Access control',
           'Rules for physical and logical access are defined and enforced based on business need.', 'Organizational controls'
    UNION ALL SELECT 'ISO27001', 'A.5.24', 'Incident management planning',
           'Responsibilities and procedures for handling security incidents are planned and prepared.', 'Organizational controls'
    UNION ALL SELECT 'ISO27001', 'A.5.30', 'ICT readiness for business continuity',
           'ICT services are planned, tested and maintained so they remain available during disruption.', 'Organizational controls'
    UNION ALL SELECT 'ISO27001', 'A.8.7', 'Protection against malware',
           'Malware protection is implemented and supported by user awareness.', 'Technological controls'
    UNION ALL SELECT 'ISO27001', 'A.8.8', 'Management of technical vulnerabilities',
           'Technical vulnerabilities are identified, evaluated and remediated in a timely manner.', 'Technological controls'
    UNION ALL SELECT 'ISO27001', 'A.8.9', 'Configuration management',
           'Secure configurations of hardware, software and networks are defined and monitored.', 'Technological controls'
    UNION ALL SELECT 'ISO27001', 'A.8.13', 'Information backup',
           'Backup copies of information and systems are maintained and regularly tested.', 'Technological controls'
    UNION ALL SELECT 'ISO27001', 'A.8.15', 'Logging',
           'Activity, exception and security event logs are produced, protected and retained.', 'Technological controls'
    UNION ALL SELECT 'ISO27001', 'A.8.16', 'Monitoring activities',
           'Networks and systems are monitored for anomalous behaviour.', 'Technological controls'
    UNION ALL SELECT 'NIST_CSF', 'GV.PO-01', 'Cybersecurity policy established',
           'A cybersecurity risk management policy is established and communicated.', 'Govern'
    UNION ALL SELECT 'NIST_CSF', 'ID.AM-01', 'Hardware inventory maintained',
           'Inventories of hardware managed by the organisation are maintained.', 'Identify'
    UNION ALL SELECT 'NIST_CSF', 'ID.AM-02', 'Software inventory maintained',
           'Inventories of software and services managed by the organisation are maintained.', 'Identify'
    UNION ALL SELECT 'NIST_CSF', 'ID.RA-01', 'Vulnerabilities identified',
           'Vulnerabilities in assets are identified, validated and recorded.', 'Identify'
    UNION ALL SELECT 'NIST_CSF', 'PR.AA-05', 'Access permissions managed',
           'Access permissions are defined, managed and reviewed using least privilege.', 'Protect'
    UNION ALL SELECT 'NIST_CSF', 'PR.DS-11', 'Backups performed and tested',
           'Backups of data are created, protected, maintained and tested.', 'Protect'
    UNION ALL SELECT 'NIST_CSF', 'DE.CM-01', 'Networks monitored',
           'Networks and network services are monitored to find potentially adverse events.', 'Detect'
    UNION ALL SELECT 'NIST_CSF', 'RS.MA-01', 'Incident response plan executed',
           'The incident response plan is executed with relevant parties once an incident is declared.', 'Respond'
    UNION ALL SELECT 'NIST_CSF', 'RC.RP-01', 'Recovery plan executed',
           'The recovery portion of the incident response plan is executed.', 'Recover'
) c ON c.fw = f.code;


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

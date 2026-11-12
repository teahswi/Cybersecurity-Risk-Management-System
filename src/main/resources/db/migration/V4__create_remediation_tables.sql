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

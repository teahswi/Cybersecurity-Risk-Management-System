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

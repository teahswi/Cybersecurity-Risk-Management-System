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

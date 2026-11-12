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

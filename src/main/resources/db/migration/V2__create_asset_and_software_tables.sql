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

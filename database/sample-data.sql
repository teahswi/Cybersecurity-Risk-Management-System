-- =====================================================================
-- CyberShield - REFERENCE SAMPLE DATA SCRIPT
--
-- In normal use you do NOT need this file: when the application starts with the dev profile,
-- DemoDataInitializer creates the demo users (hashing their passwords with BCrypt at runtime) and runs
-- src/main/resources/demo/demo-data.sql. This file contains exactly the same data for manual inspection
-- or for loading into a standalone database created with schema.sql.
--
-- Every statement is idempotent (INSERT ... SELECT ... WHERE NOT EXISTS), so running it twice does not
-- create duplicates. Passwords below are BCrypt hashes, never plain text:
--   analyst / Analyst@123, engineer / Engineer@123, auditor / Auditor@123, manager / Manager@123,
--   analyst2 and engineer2 / Demo@12345  (development demonstration credentials only)
-- Note: the sample evidence FILES are created by the application, not by this script.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Demo users (BCrypt cost 10)
-- ---------------------------------------------------------------------
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Ananya Rao','analyst','analyst@cybershield.local','$2a$10$PQyul7enVFGOGSj3veVD4e6FL3vTz2hAw3hsP.y6yr7ReaEBTOrJO',(SELECT id FROM roles WHERE code='SECURITY_ANALYST'),(SELECT id FROM departments WHERE code='SEC'),'Security Analyst','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='analyst');
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Vikram Singh','engineer','engineer@cybershield.local','$2a$10$HoBD68ErMv71fpTOo5tnPu.mJbVVxfNmIkCbkjm5ezGNt1hAo.s6u',(SELECT id FROM roles WHERE code='IT_INFRASTRUCTURE_ENGINEER'),(SELECT id FROM departments WHERE code='IT'),'Infrastructure Engineer','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='engineer');
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Meera Iyer','auditor','auditor@cybershield.local','$2a$10$Id1uyZw7Nlhwk.xH5oUv0.ckS6JLrEW0HjgAjNiX2JC5MS828Ewou',(SELECT id FROM roles WHERE code='COMPLIANCE_AUDITOR'),(SELECT id FROM departments WHERE code='LEG'),'Compliance Auditor','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='auditor');
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Arjun Mehta','manager','manager@cybershield.local','$2a$10$vrcW9sqo.vA81PZUV1V4aOp9ahnWgMv/43lyBnP5Psvv/Q1Ir9d8.',(SELECT id FROM roles WHERE code='SECURITY_MANAGER'),(SELECT id FROM departments WHERE code='SEC'),'Security Manager','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='manager');
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Neha Kapoor','analyst2','analyst2@cybershield.local','$2a$10$cjrKe771CBD7XXwalqOCuONHRaw1Sor7dOsplHA6FIMMa667T8AGG',(SELECT id FROM roles WHERE code='SECURITY_ANALYST'),(SELECT id FROM departments WHERE code='SEC'),'Senior Security Analyst','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='analyst2');
INSERT INTO users (full_name, username, email, password_hash, role_id, department_id, job_title, status, created_at, updated_at)
SELECT 'Rahul Verma','engineer2','engineer2@cybershield.local','$2a$10$cjrKe771CBD7XXwalqOCuONHRaw1Sor7dOsplHA6FIMMa667T8AGG',(SELECT id FROM roles WHERE code='IT_INFRASTRUCTURE_ENGINEER'),(SELECT id FROM departments WHERE code='IT'),'Network Engineer','ACTIVE',NOW(),NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='engineer2');

-- =====================================================================
-- CyberShield DEMONSTRATION DATA (dev profile only)
-- Executed by DemoDataService AFTER the demo users exist.
-- Every statement is idempotent (INSERT ... SELECT ... WHERE NOT EXISTS), so restarting the
-- application never creates duplicates and never overwrites records you have edited.
-- All vulnerabilities, alerts and incidents below are SAMPLE DEMONSTRATION DATA.
-- No real CVE identifiers are used (cve_id is NULL).
-- Dates are relative to the day the data is first loaded.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Assets (SRV-104 is the main end-to-end demonstration asset)
-- ---------------------------------------------------------------------
INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-104',
       'Finance Production Server',
       'SERVER',
       'fin-prod-104.corp.cybershield.local',
       '10.10.4.104',
       'Ubuntu Server 22.04 LTS',
       (SELECT id FROM departments WHERE code = 'FIN'),
       (SELECT id FROM office_locations WHERE code = 'HYD'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'HIGH',
       'PRODUCTION',
       'ACTIVE',
       'Hosts the finance web application used for invoicing and payments.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-104');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-101',
       'HR Payroll Server',
       'SERVER',
       'hr-pay-101.corp.cybershield.local',
       '10.10.4.101',
       'Windows Server 2022',
       (SELECT id FROM departments WHERE code = 'HR'),
       (SELECT id FROM office_locations WHERE code = 'HYD'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'HIGH',
       'PRODUCTION',
       'ACTIVE',
       'Payroll processing and HR self-service portal.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-101');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-110',
       'Customer Web Portal',
       'SERVER',
       'portal-110.corp.cybershield.local',
       '10.20.1.110',
       'Ubuntu Server 22.04 LTS',
       (SELECT id FROM departments WHERE code = 'ENG'),
       (SELECT id FROM office_locations WHERE code = 'BLR'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'CRITICAL',
       'PRODUCTION',
       'ACTIVE',
       'Internet-facing customer portal.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-110');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-115',
       'Internal Git Server',
       'SERVER',
       'git-115.corp.cybershield.local',
       '10.20.1.115',
       'Rocky Linux 9',
       (SELECT id FROM departments WHERE code = 'ENG'),
       (SELECT id FROM office_locations WHERE code = 'BLR'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'MEDIUM',
       'PRODUCTION',
       'ACTIVE',
       'Source code hosting for engineering teams.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-115');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-120',
       'Staging Application Server',
       'SERVER',
       'stg-app-120.corp.cybershield.local',
       '10.30.2.120',
       'Ubuntu Server 24.04 LTS',
       (SELECT id FROM departments WHERE code = 'ENG'),
       (SELECT id FROM office_locations WHERE code = 'PUN'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'MEDIUM',
       'STAGING',
       'ACTIVE',
       'Pre-production test environment.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-120');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'DB-201',
       'Finance ERP Database',
       'DATABASE',
       'erp-db-201.corp.cybershield.local',
       '10.10.5.201',
       'Oracle Linux 8',
       (SELECT id FROM departments WHERE code = 'FIN'),
       (SELECT id FROM office_locations WHERE code = 'HYD'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'CRITICAL',
       'PRODUCTION',
       'ACTIVE',
       'Primary database for the ERP system.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'DB-201');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'CLD-301',
       'Analytics Cloud VM',
       'CLOUD_INSTANCE',
       'analytics-301.cloud.cybershield.local',
       '172.31.10.30',
       'Amazon Linux 2023',
       (SELECT id FROM departments WHERE code = 'OPS'),
       (SELECT id FROM office_locations WHERE code = 'BLR'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'MEDIUM',
       'PRODUCTION',
       'ACTIVE',
       'Cloud virtual machine running reporting jobs.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'CLD-301');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'NET-401',
       'Core Firewall Hyderabad',
       'NETWORK_DEVICE',
       'fw-hyd-401.corp.cybershield.local',
       '10.10.0.1',
       'Vendor firewall OS',
       (SELECT id FROM departments WHERE code = 'IT'),
       (SELECT id FROM office_locations WHERE code = 'HYD'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'CRITICAL',
       'PRODUCTION',
       'ACTIVE',
       'Perimeter firewall for the Hyderabad headquarters.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'NET-401');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'NET-402',
       'Branch Router Pune',
       'NETWORK_DEVICE',
       'rtr-pun-402.corp.cybershield.local',
       '10.30.0.1',
       'Vendor router OS',
       (SELECT id FROM departments WHERE code = 'IT'),
       (SELECT id FROM office_locations WHERE code = 'PUN'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'HIGH',
       'PRODUCTION',
       'ACTIVE',
       'WAN edge router for the Pune office.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'NET-402');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'WKS-501',
       'Finance Analyst Workstation',
       'WORKSTATION',
       'fin-wks-501.corp.cybershield.local',
       '10.10.8.51',
       'Windows 11 Enterprise',
       (SELECT id FROM departments WHERE code = 'FIN'),
       (SELECT id FROM office_locations WHERE code = 'HYD'),
       NULL,
       'LOW',
       'PRODUCTION',
       'ACTIVE',
       'Desktop used by the finance analytics team.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'WKS-501');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'LTP-601',
       'Sales Laptop',
       'LAPTOP',
       'sal-ltp-601.corp.cybershield.local',
       '10.30.8.61',
       'Windows 11 Pro',
       (SELECT id FROM departments WHERE code = 'SAL'),
       (SELECT id FROM office_locations WHERE code = 'PUN'),
       NULL,
       'LOW',
       'PRODUCTION',
       'ACTIVE',
       'Laptop issued to the regional sales team.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'LTP-601');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'DEV-701',
       'Developer Test VM',
       'CLOUD_INSTANCE',
       'dev-701.cloud.cybershield.local',
       '172.31.20.71',
       'Ubuntu Server 24.04 LTS',
       (SELECT id FROM departments WHERE code = 'ENG'),
       (SELECT id FROM office_locations WHERE code = 'BLR'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'LOW',
       'DEVELOPMENT',
       'MAINTENANCE',
       'Short-lived development virtual machine.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'DEV-701');

INSERT INTO assets (asset_code, name, asset_type, hostname, ip_address, operating_system, department_id, location_id, owner_id, criticality, environment, status, description, created_at, updated_at)
SELECT 'SRV-130',
       'Legacy File Server',
       'SERVER',
       'files-130.corp.cybershield.local',
       '10.30.4.130',
       'Windows Server 2012 R2',
       (SELECT id FROM departments WHERE code = 'OPS'),
       (SELECT id FROM office_locations WHERE code = 'PUN'),
       NULL,
       'MEDIUM',
       'PRODUCTION',
       'ARCHIVED',
       'Decommissioned file server kept for historical records.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM assets WHERE asset_code = 'SRV-130');

-- ---------------------------------------------------------------------
-- Software catalogue (baseline_version = company-approved minimum version)
-- ---------------------------------------------------------------------
INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'OpenSSH',
       'OpenBSD Project',
       'NETWORKING',
       '9.6',
       'Secure remote administration.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'OpenSSH' AND vendor = 'OpenBSD Project');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'Apache HTTP Server',
       'Apache Software Foundation',
       'WEB_SERVER',
       '2.4.62',
       'Web server used by internal and external sites.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'Apache HTTP Server' AND vendor = 'Apache Software Foundation');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'MySQL Server',
       'Oracle',
       'DATABASE',
       '8.0.40',
       'Relational database server.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'MySQL Server' AND vendor = 'Oracle');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'Nginx',
       'F5',
       'WEB_SERVER',
       '1.26.2',
       'Reverse proxy and load balancer.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'Nginx' AND vendor = 'F5');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'OpenJDK Runtime',
       'Eclipse Adoptium',
       'APPLICATION_SERVER',
       '21.0.5',
       'Java runtime for in-house applications.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'OpenJDK Runtime' AND vendor = 'Eclipse Adoptium');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'Git',
       'Git Project',
       'DEVELOPMENT',
       '2.47.0',
       'Version control system.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'Git' AND vendor = 'Git Project');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'Google Chrome',
       'Google',
       'BROWSER',
       '130.0',
       'Standard corporate web browser.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'Google Chrome' AND vendor = 'Google');

INSERT INTO software (name, vendor, category, baseline_version, description, created_at, updated_at)
SELECT 'Microsoft Office',
       'Microsoft',
       'PRODUCTIVITY',
       '16.0',
       'Office productivity suite.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM software WHERE name = 'Microsoft Office' AND vendor = 'Microsoft');

-- ---------------------------------------------------------------------
-- Installed software (asset_software junction)
-- ---------------------------------------------------------------------
INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-104'),
       (SELECT id FROM software WHERE name = 'OpenSSH'),
       '8.9',
       DATE_ADD(CURDATE(), INTERVAL -400 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-104') AND software_id = (SELECT id FROM software WHERE name = 'OpenSSH'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-104'),
       (SELECT id FROM software WHERE name = 'Apache HTTP Server'),
       '2.4.52',
       DATE_ADD(CURDATE(), INTERVAL -400 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-104') AND software_id = (SELECT id FROM software WHERE name = 'Apache HTTP Server'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-104'),
       (SELECT id FROM software WHERE name = 'OpenJDK Runtime'),
       '17.0.8',
       DATE_ADD(CURDATE(), INTERVAL -400 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-104') AND software_id = (SELECT id FROM software WHERE name = 'OpenJDK Runtime'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       (SELECT id FROM software WHERE name = 'Nginx'),
       '1.24.0',
       DATE_ADD(CURDATE(), INTERVAL -300 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-110') AND software_id = (SELECT id FROM software WHERE name = 'Nginx'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       (SELECT id FROM software WHERE name = 'OpenJDK Runtime'),
       '21.0.5',
       DATE_ADD(CURDATE(), INTERVAL -120 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-110') AND software_id = (SELECT id FROM software WHERE name = 'OpenJDK Runtime'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-115'),
       (SELECT id FROM software WHERE name = 'Git'),
       '2.47.0',
       DATE_ADD(CURDATE(), INTERVAL -90 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-115') AND software_id = (SELECT id FROM software WHERE name = 'Git'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-115'),
       (SELECT id FROM software WHERE name = 'OpenSSH'),
       '8.7',
       DATE_ADD(CURDATE(), INTERVAL -500 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-115') AND software_id = (SELECT id FROM software WHERE name = 'OpenSSH'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-120'),
       (SELECT id FROM software WHERE name = 'OpenSSH'),
       '9.6',
       DATE_ADD(CURDATE(), INTERVAL -60 DAY),
       'OPEN_SOURCE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-120') AND software_id = (SELECT id FROM software WHERE name = 'OpenSSH'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'DB-201'),
       (SELECT id FROM software WHERE name = 'MySQL Server'),
       '8.0.32',
       DATE_ADD(CURDATE(), INTERVAL -700 DAY),
       'ENTERPRISE',
       DATE_ADD(CURDATE(), INTERVAL 365 DAY),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'DB-201') AND software_id = (SELECT id FROM software WHERE name = 'MySQL Server'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-101'),
       (SELECT id FROM software WHERE name = 'Microsoft Office'),
       '16.0',
       DATE_ADD(CURDATE(), INTERVAL -200 DAY),
       'SUBSCRIPTION',
       DATE_ADD(CURDATE(), INTERVAL 165 DAY),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-101') AND software_id = (SELECT id FROM software WHERE name = 'Microsoft Office'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'WKS-501'),
       (SELECT id FROM software WHERE name = 'Google Chrome'),
       '118.0',
       DATE_ADD(CURDATE(), INTERVAL -250 DAY),
       'FREEWARE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'WKS-501') AND software_id = (SELECT id FROM software WHERE name = 'Google Chrome'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'WKS-501'),
       (SELECT id FROM software WHERE name = 'Microsoft Office'),
       '16.0',
       DATE_ADD(CURDATE(), INTERVAL -250 DAY),
       'SUBSCRIPTION',
       DATE_ADD(CURDATE(), INTERVAL 115 DAY),
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'WKS-501') AND software_id = (SELECT id FROM software WHERE name = 'Microsoft Office'));

INSERT INTO asset_software (asset_id, software_id, installed_version, installation_date, license_type, license_expiry_date, created_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'LTP-601'),
       (SELECT id FROM software WHERE name = 'Google Chrome'),
       '130.0',
       DATE_ADD(CURDATE(), INTERVAL -30 DAY),
       'FREEWARE',
       NULL,
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_software WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'LTP-601') AND software_id = (SELECT id FROM software WHERE name = 'Google Chrome'));

-- ---------------------------------------------------------------------
-- Vulnerability definitions - SAMPLE DEMONSTRATION DATA, no real CVEs
-- ---------------------------------------------------------------------
INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0001',
       '[DEMO] Remote code execution in outdated web application framework',
       'Sample demonstration vulnerability. An outdated web framework module allows crafted HTTP requests to execute code on the server. This is not a real CVE.',
       NULL,
       'CRITICAL',
       9.8,
       'Upgrade the framework module to the approved baseline version, restart the service and verify with an authenticated scan.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0001');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0002',
       '[DEMO] Weak SSH key exchange algorithms enabled',
       'Sample demonstration vulnerability. The SSH daemon still offers legacy key exchange algorithms. This is not a real CVE.',
       NULL,
       'MEDIUM',
       5.3,
       'Restrict KexAlgorithms to modern algorithms in sshd_config and upgrade OpenSSH to the baseline version.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0002');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0003',
       '[DEMO] Database server missing quarterly security patch bundle',
       'Sample demonstration vulnerability. The database server has not received the latest vendor patch bundle. This is not a real CVE.',
       NULL,
       'HIGH',
       8.1,
       'Apply the latest patch bundle during the maintenance window and confirm the version string.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0003');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0004',
       '[DEMO] Default administrative credentials on network device',
       'Sample demonstration vulnerability. The management interface accepts factory default credentials. This is not a real CVE.',
       NULL,
       'CRITICAL',
       9.1,
       'Change the administrative password, restrict management access to the admin VLAN and enable MFA where supported.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0004');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0005',
       '[DEMO] TLS 1.0 and TLS 1.1 protocols enabled',
       'Sample demonstration vulnerability. Deprecated TLS protocol versions are accepted. This is not a real CVE.',
       NULL,
       'MEDIUM',
       5.9,
       'Disable TLS 1.0/1.1 and allow only TLS 1.2 and TLS 1.3 with strong cipher suites.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0005');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0006',
       '[DEMO] Outdated web browser with exploitable flaws',
       'Sample demonstration vulnerability. The installed browser version is below the approved baseline. This is not a real CVE.',
       NULL,
       'HIGH',
       7.5,
       'Update the browser to the baseline version and enable automatic updates through endpoint management.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0006');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0007',
       '[DEMO] Excessive file share permissions',
       'Sample demonstration vulnerability. A network share grants write access to all authenticated users. This is not a real CVE.',
       NULL,
       'LOW',
       3.7,
       'Restrict share permissions to the owning team and review access quarterly.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0007');

INSERT INTO vulnerabilities (vuln_code, title, description, cve_id, severity, cvss_score, recommended_remediation, created_by, created_at, updated_at)
SELECT 'VUL-0008',
       '[DEMO] Host-based firewall rules missing',
       'Sample demonstration vulnerability. The host firewall allows all inbound traffic from the internal network. This is not a real CVE.',
       NULL,
       'LOW',
       3.1,
       'Enable the host firewall with a default-deny inbound policy and allow only required ports.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 600 HOUR),
       DATE_SUB(NOW(), INTERVAL 600 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM vulnerabilities WHERE vuln_code = 'VUL-0008');

-- ---------------------------------------------------------------------
-- Asset vulnerability instances (one row = one vulnerability on one asset)
-- ---------------------------------------------------------------------
INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-104'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0001'),
       DATE_ADD(CURDATE(), INTERVAL -6 DAY),
       DATE_ADD(CURDATE(), INTERVAL -1 DAY),
       'OPEN',
       'Detected by the weekly authenticated scan. Finance web application exposed internally on port 443.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 144 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-104') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0001'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0001'),
       DATE_ADD(CURDATE(), INTERVAL -6 DAY),
       DATE_ADD(CURDATE(), INTERVAL -1 DAY),
       'MITIGATING',
       'Same framework version as SRV-104; internet facing, remediation in progress.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 144 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-110') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0001'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-115'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0002'),
       DATE_ADD(CURDATE(), INTERVAL -12 DAY),
       DATE_ADD(CURDATE(), INTERVAL -2 DAY),
       'OPEN',
       'Legacy key exchange algorithms offered.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 288 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-115') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0002'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-120'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0002'),
       DATE_ADD(CURDATE(), INTERVAL -12 DAY),
       DATE_ADD(CURDATE(), INTERVAL -2 DAY),
       'OPEN',
       'Staging server cloned from the Git server image.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 288 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-120') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0002'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'DB-201'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0003'),
       DATE_ADD(CURDATE(), INTERVAL -20 DAY),
       DATE_ADD(CURDATE(), INTERVAL -3 DAY),
       'MITIGATING',
       'Patch applied during the weekend maintenance window; awaiting verification.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 480 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'DB-201') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0003'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-101'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0003'),
       DATE_ADD(CURDATE(), INTERVAL -20 DAY),
       DATE_ADD(CURDATE(), INTERVAL -3 DAY),
       'OPEN',
       'Payroll database instance on the HR server.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 480 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-101') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0003'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'NET-402'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0004'),
       DATE_ADD(CURDATE(), INTERVAL -30 DAY),
       DATE_ADD(CURDATE(), INTERVAL -25 DAY),
       'MITIGATED',
       'Default credentials replaced; verified by the security team.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'NET-402') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0004'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0005'),
       DATE_ADD(CURDATE(), INTERVAL -15 DAY),
       DATE_ADD(CURDATE(), INTERVAL -2 DAY),
       'OPEN',
       'Load balancer still accepts TLS 1.0.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 360 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-110') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0005'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'NET-401'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0005'),
       DATE_ADD(CURDATE(), INTERVAL -15 DAY),
       DATE_ADD(CURDATE(), INTERVAL -2 DAY),
       'ACCEPTED',
       'Risk accepted until firewall replacement next quarter (see RSK-0004).',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 360 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'NET-401') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0005'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'WKS-501'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0006'),
       DATE_ADD(CURDATE(), INTERVAL -10 DAY),
       DATE_ADD(CURDATE(), INTERVAL -1 DAY),
       'OPEN',
       'Browser auto-update disabled by a local policy.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 240 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'WKS-501') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0006'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'LTP-601'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0006'),
       DATE_ADD(CURDATE(), INTERVAL -10 DAY),
       DATE_ADD(CURDATE(), INTERVAL -9 DAY),
       'CLOSED',
       'Browser updated by the user; re-scan clean.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 240 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'LTP-601') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0006'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'SRV-130'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0007'),
       DATE_ADD(CURDATE(), INTERVAL -60 DAY),
       DATE_ADD(CURDATE(), INTERVAL -40 DAY),
       'CLOSED',
       'Server archived; share removed.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 1440 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'SRV-130') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0007'));

INSERT INTO asset_vulnerabilities (asset_id, vulnerability_id, discovered_date, last_observed_date, status, notes, created_by, created_at, updated_at)
SELECT (SELECT id FROM assets WHERE asset_code = 'CLD-301'),
       (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0008'),
       DATE_ADD(CURDATE(), INTERVAL -8 DAY),
       DATE_ADD(CURDATE(), INTERVAL -1 DAY),
       'OPEN',
       'Security group allows all internal traffic.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 192 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM asset_vulnerabilities WHERE asset_id = (SELECT id FROM assets WHERE asset_code = 'CLD-301') AND vulnerability_id = (SELECT id FROM vulnerabilities WHERE vuln_code = 'VUL-0008'));

-- ---------------------------------------------------------------------
-- Remediation tasks in different workflow states (SRV-104 deliberately has NO task yet)
-- ---------------------------------------------------------------------
INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0001',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-110' AND v.vuln_code = 'VUL-0001'),
       'Upgrade web framework on customer portal',
       'Upgrade the framework module on SRV-110 to the approved baseline and restart the portal service.',
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_ADD(CURDATE(), INTERVAL 5 DAY),
       'CRITICAL',
       'IN_PROGRESS',
       NULL,
       DATE_SUB(NOW(), INTERVAL 96 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0001');

INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0002',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-115' AND v.vuln_code = 'VUL-0002'),
       'Harden SSH configuration on Git server',
       'Disable legacy key exchange algorithms and upgrade OpenSSH on SRV-115.',
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_ADD(CURDATE(), INTERVAL 10 DAY),
       'MEDIUM',
       'ASSIGNED',
       NULL,
       DATE_SUB(NOW(), INTERVAL 48 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0002');

INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0003',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'DB-201' AND v.vuln_code = 'VUL-0003'),
       'Apply database patch bundle on ERP database',
       'Apply the quarterly patch bundle to DB-201 during the maintenance window.',
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst2'),
       DATE_ADD(CURDATE(), INTERVAL 3 DAY),
       'HIGH',
       'PENDING_VERIFICATION',
       NULL,
       DATE_SUB(NOW(), INTERVAL 200 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0003');

INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0004',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'NET-402' AND v.vuln_code = 'VUL-0004'),
       'Replace default credentials on Pune router',
       'Rotate the administrative password and restrict management access on NET-402.',
       (SELECT id FROM users WHERE username = 'engineer2'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_ADD(CURDATE(), INTERVAL -20 DAY),
       'CRITICAL',
       'COMPLETED',
       DATE_SUB(NOW(), INTERVAL 500 HOUR),
       DATE_SUB(NOW(), INTERVAL 720 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0004');

INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0005',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'WKS-501' AND v.vuln_code = 'VUL-0006'),
       'Update browser on finance workstation',
       'Update Google Chrome on WKS-501 and re-enable automatic updates.',
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_ADD(CURDATE(), INTERVAL -2 DAY),
       'HIGH',
       'ASSIGNED',
       NULL,
       DATE_SUB(NOW(), INTERVAL 240 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0005');

INSERT INTO remediation_tasks (task_code, asset_vulnerability_id, title, description, assigned_engineer_id, created_by, due_date, priority, status, completed_at, created_at, updated_at)
SELECT 'REM-0006',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-120' AND v.vuln_code = 'VUL-0002'),
       'Harden SSH configuration on staging server',
       'Apply the same SSH hardening as REM-0002 to SRV-120.',
       NULL,
       (SELECT id FROM users WHERE username = 'analyst2'),
       NULL,
       'LOW',
       'OPEN',
       NULL,
       DATE_SUB(NOW(), INTERVAL 24 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_tasks WHERE task_code = 'REM-0006');

-- ---------------------------------------------------------------------
-- Remediation history (append-only)
-- ---------------------------------------------------------------------
INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       'Task created for VUL-0001 on SRV-110.',
       DATE_SUB(NOW(), INTERVAL 96 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001') AND action_type = 'CREATED' AND description = 'Task created for VUL-0001 on SRV-110.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001'),
       'ASSIGNED',
       'OPEN',
       'ASSIGNED',
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Assigned to Vikram Singh.',
       DATE_SUB(NOW(), INTERVAL 95 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001') AND action_type = 'ASSIGNED' AND description = 'Assigned to Vikram Singh.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001'),
       'STATUS_CHANGED',
       'ASSIGNED',
       'IN_PROGRESS',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Work started.',
       DATE_SUB(NOW(), INTERVAL 70 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001') AND action_type = 'STATUS_CHANGED' AND description = 'Work started.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001'),
       'NOTE_ADDED',
       NULL,
       NULL,
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Change request approved; upgrade scheduled for tonight.',
       DATE_SUB(NOW(), INTERVAL 69 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0001') AND action_type = 'NOTE_ADDED' AND description = 'Change request approved; upgrade scheduled for tonight.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0002'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       'Task created for VUL-0002 on SRV-115.',
       DATE_SUB(NOW(), INTERVAL 48 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0002') AND action_type = 'CREATED' AND description = 'Task created for VUL-0002 on SRV-115.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0002'),
       'ASSIGNED',
       'OPEN',
       'ASSIGNED',
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Assigned to Vikram Singh.',
       DATE_SUB(NOW(), INTERVAL 47 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0002') AND action_type = 'ASSIGNED' AND description = 'Assigned to Vikram Singh.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst2'),
       'Task created for VUL-0003 on DB-201.',
       DATE_SUB(NOW(), INTERVAL 200 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'CREATED' AND description = 'Task created for VUL-0003 on DB-201.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'ASSIGNED',
       'OPEN',
       'ASSIGNED',
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst2'),
       'Assigned to Vikram Singh.',
       DATE_SUB(NOW(), INTERVAL 199 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'ASSIGNED' AND description = 'Assigned to Vikram Singh.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'STATUS_CHANGED',
       'ASSIGNED',
       'IN_PROGRESS',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Work started.',
       DATE_SUB(NOW(), INTERVAL 150 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'STATUS_CHANGED' AND description = 'Work started.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'NOTE_ADDED',
       NULL,
       NULL,
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Patch bundle applied and database restarted successfully.',
       DATE_SUB(NOW(), INTERVAL 30 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'NOTE_ADDED' AND description = 'Patch bundle applied and database restarted successfully.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'EVIDENCE_UPLOADED',
       NULL,
       NULL,
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Uploaded evidence file db201-patch-report.txt.',
       DATE_SUB(NOW(), INTERVAL 29 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'EVIDENCE_UPLOADED' AND description = 'Uploaded evidence file db201-patch-report.txt.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'EVIDENCE_SUBMITTED',
       'IN_PROGRESS',
       'PENDING_VERIFICATION',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       'Evidence submitted for verification.',
       DATE_SUB(NOW(), INTERVAL 28 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003') AND action_type = 'EVIDENCE_SUBMITTED' AND description = 'Evidence submitted for verification.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       'Task created for VUL-0004 on NET-402.',
       DATE_SUB(NOW(), INTERVAL 720 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'CREATED' AND description = 'Task created for VUL-0004 on NET-402.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'ASSIGNED',
       'OPEN',
       'ASSIGNED',
       NULL,
       (SELECT id FROM users WHERE username = 'engineer2'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Assigned to Rahul Verma.',
       DATE_SUB(NOW(), INTERVAL 719 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'ASSIGNED' AND description = 'Assigned to Rahul Verma.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'STATUS_CHANGED',
       'ASSIGNED',
       'IN_PROGRESS',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer2'),
       'Work started.',
       DATE_SUB(NOW(), INTERVAL 700 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'STATUS_CHANGED' AND description = 'Work started.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'EVIDENCE_UPLOADED',
       NULL,
       NULL,
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer2'),
       'Uploaded evidence file net402-credential-change.txt.',
       DATE_SUB(NOW(), INTERVAL 530 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'EVIDENCE_UPLOADED' AND description = 'Uploaded evidence file net402-credential-change.txt.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'EVIDENCE_SUBMITTED',
       'IN_PROGRESS',
       'PENDING_VERIFICATION',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'engineer2'),
       'Evidence submitted for verification.',
       DATE_SUB(NOW(), INTERVAL 529 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'EVIDENCE_SUBMITTED' AND description = 'Evidence submitted for verification.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'EVIDENCE_APPROVED',
       'PENDING_VERIFICATION',
       'COMPLETED',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       'Evidence approved. Vulnerability instance marked MITIGATED.',
       DATE_SUB(NOW(), INTERVAL 500 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004') AND action_type = 'EVIDENCE_APPROVED' AND description = 'Evidence approved. Vulnerability instance marked MITIGATED.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0005'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       'Task created for VUL-0006 on WKS-501.',
       DATE_SUB(NOW(), INTERVAL 240 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0005') AND action_type = 'CREATED' AND description = 'Task created for VUL-0006 on WKS-501.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0005'),
       'ASSIGNED',
       'OPEN',
       'ASSIGNED',
       NULL,
       (SELECT id FROM users WHERE username = 'engineer'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Assigned to Vikram Singh.',
       DATE_SUB(NOW(), INTERVAL 239 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0005') AND action_type = 'ASSIGNED' AND description = 'Assigned to Vikram Singh.');

INSERT INTO remediation_history (task_id, action_type, previous_status, new_status, previous_assignee_id, new_assignee_id, performed_by_id, description, created_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0006'),
       'CREATED',
       NULL,
       'OPEN',
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst2'),
       'Task created for VUL-0002 on SRV-120 (not yet assigned).',
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_history WHERE task_id = (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0006') AND action_type = 'CREATED' AND description = 'Task created for VUL-0002 on SRV-120 (not yet assigned).');

-- ---------------------------------------------------------------------
-- Remediation evidence (the files themselves are written by DemoDataService)
-- ---------------------------------------------------------------------
INSERT INTO remediation_evidence (task_id, original_filename, stored_filename, file_path, mime_type, file_size, description, status, submitted_by_id, submitted_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'db201-patch-report.txt',
       'demo-rem-0003-db201-patch-report.txt',
       'remediation/demo-rem-0003-db201-patch-report.txt',
       'text/plain',
       512,
       'Patch installation log and version check output.',
       'SUBMITTED',
       (SELECT id FROM users WHERE username = 'engineer'),
       DATE_SUB(NOW(), INTERVAL 29 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_evidence WHERE stored_filename = 'demo-rem-0003-db201-patch-report.txt');

INSERT INTO remediation_evidence (task_id, original_filename, stored_filename, file_path, mime_type, file_size, description, status, submitted_by_id, submitted_at)
SELECT (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'net402-credential-change.txt',
       'demo-rem-0004-net402-credential-change.txt',
       'remediation/demo-rem-0004-net402-credential-change.txt',
       'text/plain',
       512,
       'Change record and login test showing default credentials rejected.',
       'APPROVED',
       (SELECT id FROM users WHERE username = 'engineer2'),
       DATE_SUB(NOW(), INTERVAL 530 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_evidence WHERE stored_filename = 'demo-rem-0004-net402-credential-change.txt');

INSERT INTO remediation_evidence_reviews (evidence_id, reviewer_id, decision, comments, reviewed_at)
SELECT (SELECT id FROM remediation_evidence WHERE stored_filename = 'demo-rem-0004-net402-credential-change.txt'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'APPROVED',
       'Verified: default credentials no longer accepted.',
       DATE_SUB(NOW(), INTERVAL 500 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM remediation_evidence_reviews WHERE evidence_id = (SELECT id FROM remediation_evidence WHERE stored_filename = 'demo-rem-0004-net402-credential-change.txt'));

-- ---------------------------------------------------------------------
-- Security alerts (recorded in MySQL - no live SIEM is connected)
-- ---------------------------------------------------------------------
INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0001',
       'Multiple failed SSH logins on SRV-104',
       '42 failed SSH logins from one internal IP within 10 minutes.',
       'IDS',
       'HIGH',
       (SELECT id FROM assets WHERE asset_code = 'SRV-104'),
       DATE_SUB(NOW(), INTERVAL 5 HOUR),
       'NEW',
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 5 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0001');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0002',
       'Malware signature detected on WKS-501',
       'Antivirus quarantined a trojan dropper from a downloaded invoice attachment.',
       'ANTIVIRUS',
       'CRITICAL',
       (SELECT id FROM assets WHERE asset_code = 'WKS-501'),
       DATE_SUB(NOW(), INTERVAL 30 HOUR),
       'INVESTIGATING',
       (SELECT id FROM users WHERE username = 'analyst'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 30 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0002');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0003',
       'Suspicious outbound traffic from CLD-301',
       'Large outbound transfer to an unfamiliar external address.',
       'FIREWALL',
       'MEDIUM',
       (SELECT id FROM assets WHERE asset_code = 'CLD-301'),
       DATE_SUB(NOW(), INTERVAL 50 HOUR),
       'ESCALATED',
       (SELECT id FROM users WHERE username = 'analyst2'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 50 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0003');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0004',
       'Phishing email reported by finance user',
       'User reported an email impersonating the CFO requesting a payment.',
       'EMAIL_GATEWAY',
       'MEDIUM',
       NULL,
       DATE_SUB(NOW(), INTERVAL 32 HOUR),
       'INVESTIGATING',
       (SELECT id FROM users WHERE username = 'analyst'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 32 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0004');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0005',
       'Port scan detected against NET-401',
       'Sequential port scan from an external address blocked at the perimeter.',
       'IDS',
       'LOW',
       (SELECT id FROM assets WHERE asset_code = 'NET-401'),
       DATE_SUB(NOW(), INTERVAL 200 HOUR),
       'RESOLVED',
       (SELECT id FROM users WHERE username = 'analyst2'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 200 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0005');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0006',
       'Privilege escalation attempt on SRV-110',
       'Endpoint agent blocked an attempt to add a user to the administrators group.',
       'EDR',
       'HIGH',
       (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       DATE_SUB(NOW(), INTERVAL 8 HOUR),
       'NEW',
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 8 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0006');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0007',
       'Antivirus disabled on LTP-601',
       'Real-time protection was disabled for more than 2 hours.',
       'ANTIVIRUS',
       'MEDIUM',
       (SELECT id FROM assets WHERE asset_code = 'LTP-601'),
       DATE_SUB(NOW(), INTERVAL 300 HOUR),
       'CLOSED',
       (SELECT id FROM users WHERE username = 'analyst'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 300 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0007');

INSERT INTO security_alerts (alert_code, title, description, source, severity, asset_id, detected_at, status, assigned_analyst_id, created_by, created_at, updated_at)
SELECT 'ALT-0008',
       'Unusual database export volume on DB-201',
       'Export volume 15x higher than the 30-day baseline.',
       'DLP',
       'HIGH',
       (SELECT id FROM assets WHERE asset_code = 'DB-201'),
       DATE_SUB(NOW(), INTERVAL 48 HOUR),
       'ESCALATED',
       (SELECT id FROM users WHERE username = 'analyst2'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 48 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM security_alerts WHERE alert_code = 'ALT-0008');

-- ---------------------------------------------------------------------
-- Incidents, linked alerts, affected assets and investigation notes
-- ---------------------------------------------------------------------
INSERT INTO incidents (incident_code, title, description, incident_type, severity, status, assigned_analyst_id, detected_at, resolution_details, resolved_at, created_by, created_at, updated_at)
SELECT 'INC-0001',
       'Malware infection on finance workstation',
       'Trojan dropper delivered through a phishing email to the finance team.',
       'MALWARE',
       'HIGH',
       'INVESTIGATING',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 30 HOUR),
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 30 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE incident_code = 'INC-0001');

INSERT INTO incidents (incident_code, title, description, incident_type, severity, status, assigned_analyst_id, detected_at, resolution_details, resolved_at, created_by, created_at, updated_at)
SELECT 'INC-0002',
       'Possible data exfiltration from ERP database',
       'Unusual export volume on DB-201 correlated with outbound traffic from CLD-301.',
       'DATA_BREACH',
       'CRITICAL',
       'CONTAINED',
       (SELECT id FROM users WHERE username = 'analyst2'),
       DATE_SUB(NOW(), INTERVAL 48 HOUR),
       NULL,
       NULL,
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 48 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE incident_code = 'INC-0002');

INSERT INTO incidents (incident_code, title, description, incident_type, severity, status, assigned_analyst_id, detected_at, resolution_details, resolved_at, created_by, created_at, updated_at)
SELECT 'INC-0003',
       'Reconnaissance scan against perimeter firewall',
       'External port scan against NET-401.',
       'RECONNAISSANCE',
       'LOW',
       'RESOLVED',
       (SELECT id FROM users WHERE username = 'analyst2'),
       DATE_SUB(NOW(), INTERVAL 200 HOUR),
       'Source address blocked at the perimeter; no successful connections found in firewall logs.',
       DATE_SUB(NOW(), INTERVAL 150 HOUR),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 200 HOUR),
       DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE incident_code = 'INC-0003');

INSERT INTO incident_alerts (incident_id, alert_id, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0002'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_alerts WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0001') AND alert_id = (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0002'));

INSERT INTO incident_alerts (incident_id, alert_id, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0004'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_alerts WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0001') AND alert_id = (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0004'));

INSERT INTO incident_alerts (incident_id, alert_id, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0008'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_alerts WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND alert_id = (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0008'));

INSERT INTO incident_alerts (incident_id, alert_id, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0003'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_alerts WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND alert_id = (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0003'));

INSERT INTO incident_alerts (incident_id, alert_id, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0003'),
       (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0005'),
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_alerts WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0003') AND alert_id = (SELECT id FROM security_alerts WHERE alert_code = 'ALT-0005'));

INSERT INTO incident_assets (incident_id, asset_id, impact_description, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       (SELECT id FROM assets WHERE asset_code = 'WKS-501'),
       'Workstation isolated from the network.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_assets WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0001') AND asset_id = (SELECT id FROM assets WHERE asset_code = 'WKS-501'));

INSERT INTO incident_assets (incident_id, asset_id, impact_description, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM assets WHERE asset_code = 'DB-201'),
       'Possible unauthorised export of ERP records.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_assets WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND asset_id = (SELECT id FROM assets WHERE asset_code = 'DB-201'));

INSERT INTO incident_assets (incident_id, asset_id, impact_description, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM assets WHERE asset_code = 'CLD-301'),
       'Outbound transfer originated here.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_assets WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND asset_id = (SELECT id FROM assets WHERE asset_code = 'CLD-301'));

INSERT INTO incident_assets (incident_id, asset_id, impact_description, linked_by_id, linked_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0003'),
       (SELECT id FROM assets WHERE asset_code = 'NET-401'),
       'Scanned perimeter device; no impact.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM incident_assets WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0003') AND asset_id = (SELECT id FROM assets WHERE asset_code = 'NET-401'));

INSERT INTO investigation_notes (incident_id, author_id, note, created_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Workstation isolated via EDR. Collecting memory image for analysis.',
       DATE_SUB(NOW(), INTERVAL 28 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM investigation_notes WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0001') AND note = 'Workstation isolated via EDR. Collecting memory image for analysis.');

INSERT INTO investigation_notes (incident_id, author_id, note, created_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Phishing email purged from all mailboxes; sender domain blocked.',
       DATE_SUB(NOW(), INTERVAL 20 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM investigation_notes WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0001') AND note = 'Phishing email purged from all mailboxes; sender domain blocked.');

INSERT INTO investigation_notes (incident_id, author_id, note, created_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM users WHERE username = 'analyst2'),
       'Service account used for the export has been disabled.',
       DATE_SUB(NOW(), INTERVAL 40 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM investigation_notes WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND note = 'Service account used for the export has been disabled.');

INSERT INTO investigation_notes (incident_id, author_id, note, created_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0002'),
       (SELECT id FROM users WHERE username = 'manager'),
       'Legal team informed as a precaution. Keep the incident CONTAINED until log review is complete.',
       DATE_SUB(NOW(), INTERVAL 36 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM investigation_notes WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0002') AND note = 'Legal team informed as a precaution. Keep the incident CONTAINED until log review is complete.');

INSERT INTO investigation_notes (incident_id, author_id, note, created_at)
SELECT (SELECT id FROM incidents WHERE incident_code = 'INC-0003'),
       (SELECT id FROM users WHERE username = 'analyst2'),
       'Scan blocked; no follow-up activity observed for 24 hours.',
       DATE_SUB(NOW(), INTERVAL 160 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM investigation_notes WHERE incident_id = (SELECT id FROM incidents WHERE incident_code = 'INC-0003') AND note = 'Scan blocked; no follow-up activity observed for 24 hours.');

-- ---------------------------------------------------------------------
-- Audit plans, findings, corrective actions and audit evidence
-- ---------------------------------------------------------------------
INSERT INTO audit_plans (plan_code, name, framework_id, description, assigned_auditor_id, start_date, target_completion_date, status, created_by, created_at, updated_at)
SELECT 'AUD-0001',
       'ISO 27001 Internal Audit 2026',
       (SELECT id FROM compliance_frameworks WHERE code = 'ISO27001'),
       'Annual internal audit of selected Annex A controls.',
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_ADD(CURDATE(), INTERVAL -30 DAY),
       DATE_ADD(CURDATE(), INTERVAL 30 DAY),
       'IN_PROGRESS',
       (SELECT id FROM users WHERE username = 'manager'),
       DATE_SUB(NOW(), INTERVAL 840 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_plans WHERE plan_code = 'AUD-0001');

INSERT INTO audit_plans (plan_code, name, framework_id, description, assigned_auditor_id, start_date, target_completion_date, status, created_by, created_at, updated_at)
SELECT 'AUD-0002',
       'NIST CSF Vulnerability Management Review',
       (SELECT id FROM compliance_frameworks WHERE code = 'NIST_CSF'),
       'Review of vulnerability identification and remediation outcomes.',
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_ADD(CURDATE(), INTERVAL 7 DAY),
       DATE_ADD(CURDATE(), INTERVAL 45 DAY),
       'PLANNED',
       (SELECT id FROM users WHERE username = 'manager'),
       DATE_SUB(NOW(), INTERVAL 840 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_plans WHERE plan_code = 'AUD-0002');

INSERT INTO audit_findings (finding_code, audit_plan_id, control_id, title, description, severity, status, due_date, created_by, created_at, updated_at)
SELECT 'FND-0001',
       (SELECT id FROM audit_plans WHERE plan_code = 'AUD-0001'),
       (SELECT cc.id FROM compliance_controls cc JOIN compliance_frameworks f ON f.id = cc.framework_id WHERE f.code = 'ISO27001' AND cc.control_code = 'A.8.8'),
       'Patch SLA not met for critical servers',
       'Two critical servers exceeded the 15-day patch window for critical vulnerabilities.',
       'HIGH',
       'IN_PROGRESS',
       DATE_ADD(CURDATE(), INTERVAL 20 DAY),
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 480 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_findings WHERE finding_code = 'FND-0001');

INSERT INTO audit_findings (finding_code, audit_plan_id, control_id, title, description, severity, status, due_date, created_by, created_at, updated_at)
SELECT 'FND-0002',
       (SELECT id FROM audit_plans WHERE plan_code = 'AUD-0001'),
       (SELECT cc.id FROM compliance_controls cc JOIN compliance_frameworks f ON f.id = cc.framework_id WHERE f.code = 'ISO27001' AND cc.control_code = 'A.8.15'),
       'Log retention period not documented',
       'The retention period for security logs is not defined in any approved policy.',
       'MEDIUM',
       'IN_PROGRESS',
       DATE_ADD(CURDATE(), INTERVAL 30 DAY),
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 480 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_findings WHERE finding_code = 'FND-0002');

INSERT INTO audit_findings (finding_code, audit_plan_id, control_id, title, description, severity, status, due_date, created_by, created_at, updated_at)
SELECT 'FND-0003',
       (SELECT id FROM audit_plans WHERE plan_code = 'AUD-0001'),
       (SELECT cc.id FROM compliance_controls cc JOIN compliance_frameworks f ON f.id = cc.framework_id WHERE f.code = 'ISO27001' AND cc.control_code = 'A.5.15'),
       'Stale accounts in HR application',
       'Three accounts of former employees were still active.',
       'LOW',
       'RESOLVED',
       DATE_ADD(CURDATE(), INTERVAL -5 DAY),
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 480 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_findings WHERE finding_code = 'FND-0003');

INSERT INTO corrective_actions (finding_id, assigned_to_id, description, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM audit_findings WHERE finding_code = 'FND-0001'),
       (SELECT id FROM users WHERE username = 'engineer'),
       'Define a 15-day patch SLA for critical servers and add SLA tracking to the remediation process.',
       DATE_ADD(CURDATE(), INTERVAL 15 DAY),
       'IN_PROGRESS',
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 432 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0001') AND description = 'Define a 15-day patch SLA for critical servers and add SLA tracking to the remediation process.');

INSERT INTO corrective_actions (finding_id, assigned_to_id, description, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM audit_findings WHERE finding_code = 'FND-0002'),
       (SELECT id FROM users WHERE username = 'analyst'),
       'Document the security log retention period and get it approved by the Security Manager.',
       DATE_ADD(CURDATE(), INTERVAL 25 DAY),
       'PENDING_REVIEW',
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 432 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0002') AND description = 'Document the security log retention period and get it approved by the Security Manager.');

INSERT INTO corrective_actions (finding_id, assigned_to_id, description, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM audit_findings WHERE finding_code = 'FND-0003'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       'Disable the stale accounts and add a monthly leaver account review.',
       DATE_ADD(CURDATE(), INTERVAL -10 DAY),
       'VERIFIED',
       (SELECT id FROM users WHERE username = 'auditor'),
       DATE_SUB(NOW(), INTERVAL 432 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0003') AND description = 'Disable the stale accounts and add a monthly leaver account review.');

INSERT INTO audit_evidence (corrective_action_id, original_filename, stored_filename, file_path, mime_type, file_size, description, status, submitted_by_id, submitted_at)
SELECT (SELECT id FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0002') AND description = 'Document the security log retention period and get it approved by the Security Manager.'),
       'log-retention-policy-draft.txt',
       'demo-audit-log-retention-policy.txt',
       'audit/demo-audit-log-retention-policy.txt',
       'text/plain',
       512,
       'Supporting document for the corrective action.',
       'SUBMITTED',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 20 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_evidence WHERE stored_filename = 'demo-audit-log-retention-policy.txt');

INSERT INTO audit_evidence (corrective_action_id, original_filename, stored_filename, file_path, mime_type, file_size, description, status, submitted_by_id, submitted_at)
SELECT (SELECT id FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0003') AND description = 'Disable the stale accounts and add a monthly leaver account review.'),
       'leaver-review-record.txt',
       'demo-audit-leaver-review.txt',
       'audit/demo-audit-leaver-review.txt',
       'text/plain',
       512,
       'Supporting document for the corrective action.',
       'VERIFIED',
       (SELECT id FROM users WHERE username = 'engineer2'),
       DATE_SUB(NOW(), INTERVAL 150 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_evidence WHERE stored_filename = 'demo-audit-leaver-review.txt');

INSERT INTO audit_evidence_reviews (evidence_id, reviewer_id, decision, review_notes, reviewed_at)
SELECT (SELECT id FROM audit_evidence WHERE stored_filename = 'demo-audit-leaver-review.txt'),
       (SELECT id FROM users WHERE username = 'auditor'),
       'VERIFIED',
       'Accounts confirmed disabled in the HR application.',
       DATE_SUB(NOW(), INTERVAL 140 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_evidence_reviews WHERE evidence_id = (SELECT id FROM audit_evidence WHERE stored_filename = 'demo-audit-leaver-review.txt'));

-- ---------------------------------------------------------------------
-- Risk assessments (score = likelihood x impact) and treatments
-- ---------------------------------------------------------------------
INSERT INTO risk_assessments (risk_code, title, description, asset_id, asset_vulnerability_id, likelihood, impact, risk_score, risk_level, treatment_strategy, owner_id, status, created_by, created_at, updated_at)
SELECT 'RSK-0001',
       'Remote compromise of customer portal',
       'Exploitation of VUL-0001 could expose customer accounts.',
       (SELECT id FROM assets WHERE asset_code = 'SRV-110'),
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-110' AND v.vuln_code = 'VUL-0001'),
       4,
       5,
       20,
       'CRITICAL',
       'MITIGATE',
       (SELECT id FROM users WHERE username = 'analyst'),
       'IN_TREATMENT',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 120 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_assessments WHERE risk_code = 'RSK-0001');

INSERT INTO risk_assessments (risk_code, title, description, asset_id, asset_vulnerability_id, likelihood, impact, risk_score, risk_level, treatment_strategy, owner_id, status, created_by, created_at, updated_at)
SELECT 'RSK-0002',
       'Leakage of ERP financial data',
       'Unpatched database combined with excessive export rights.',
       (SELECT id FROM assets WHERE asset_code = 'DB-201'),
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'DB-201' AND v.vuln_code = 'VUL-0003'),
       3,
       5,
       15,
       'HIGH',
       'MITIGATE',
       (SELECT id FROM users WHERE username = 'analyst2'),
       'TREATMENT_PLANNED',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 120 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_assessments WHERE risk_code = 'RSK-0002');

INSERT INTO risk_assessments (risk_code, title, description, asset_id, asset_vulnerability_id, likelihood, impact, risk_score, risk_level, treatment_strategy, owner_id, status, created_by, created_at, updated_at)
SELECT 'RSK-0003',
       'Weak SSH configuration on Git server',
       'Downgrade attacks against administrative SSH sessions.',
       (SELECT id FROM assets WHERE asset_code = 'SRV-115'),
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-115' AND v.vuln_code = 'VUL-0002'),
       2,
       3,
       6,
       'MEDIUM',
       'MITIGATE',
       (SELECT id FROM users WHERE username = 'analyst'),
       'OPEN',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 120 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_assessments WHERE risk_code = 'RSK-0003');

INSERT INTO risk_assessments (risk_code, title, description, asset_id, asset_vulnerability_id, likelihood, impact, risk_score, risk_level, treatment_strategy, owner_id, status, created_by, created_at, updated_at)
SELECT 'RSK-0004',
       'Legacy TLS on perimeter firewall',
       'Legacy TLS on the management interface; device is scheduled for replacement.',
       (SELECT id FROM assets WHERE asset_code = 'NET-401'),
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'NET-401' AND v.vuln_code = 'VUL-0005'),
       2,
       2,
       4,
       'LOW',
       'ACCEPT',
       (SELECT id FROM users WHERE username = 'manager'),
       'MONITORING',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 120 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_assessments WHERE risk_code = 'RSK-0004');

INSERT INTO risk_treatments (risk_id, strategy, action_description, owner_id, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0001'),
       'MITIGATE',
       'Patch the framework (REM-0001) and add a temporary WAF rule.',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_ADD(CURDATE(), INTERVAL 5 DAY),
       'IN_PROGRESS',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 96 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_treatments WHERE risk_id = (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0001') AND action_description = 'Patch the framework (REM-0001) and add a temporary WAF rule.');

INSERT INTO risk_treatments (risk_id, strategy, action_description, owner_id, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0002'),
       'MITIGATE',
       'Apply the patch bundle (REM-0003) and restrict export permissions.',
       (SELECT id FROM users WHERE username = 'analyst2'),
       DATE_ADD(CURDATE(), INTERVAL 10 DAY),
       'PLANNED',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 96 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_treatments WHERE risk_id = (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0002') AND action_description = 'Apply the patch bundle (REM-0003) and restrict export permissions.');

INSERT INTO risk_treatments (risk_id, strategy, action_description, owner_id, deadline, status, created_by, created_at, updated_at)
SELECT (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0004'),
       'ACCEPT',
       'Accept until hardware replacement; review monthly.',
       (SELECT id FROM users WHERE username = 'manager'),
       DATE_ADD(CURDATE(), INTERVAL 60 DAY),
       'IN_PROGRESS',
       (SELECT id FROM users WHERE username = 'analyst'),
       DATE_SUB(NOW(), INTERVAL 96 HOUR),
       DATE_SUB(NOW(), INTERVAL 24 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM risk_treatments WHERE risk_id = (SELECT id FROM risk_assessments WHERE risk_code = 'RSK-0004') AND action_description = 'Accept until hardware replacement; review monthly.');

-- ---------------------------------------------------------------------
-- Notifications (dedup_key keeps this idempotent)
-- ---------------------------------------------------------------------
INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'engineer'),
       'TASK_ASSIGNED',
       'New remediation task REM-0002',
       'Harden SSH configuration on Git server (SRV-115).',
       'REMEDIATION_TASK',
       (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0002'),
       'DEMO:TASK_ASSIGNED:REM-0002',
       DATE_SUB(NOW(), INTERVAL 47 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'engineer') AND dedup_key = 'DEMO:TASK_ASSIGNED:REM-0002');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'engineer'),
       'TASK_OVERDUE',
       'Task REM-0005 is overdue',
       'Update browser on finance workstation was due 2 days ago.',
       'REMEDIATION_TASK',
       (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0005'),
       'DEMO:TASK_OVERDUE:REM-0005',
       DATE_SUB(NOW(), INTERVAL 2 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'engineer') AND dedup_key = 'DEMO:TASK_OVERDUE:REM-0005');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'analyst2'),
       'EVIDENCE_SUBMITTED',
       'Evidence submitted for REM-0003',
       'Vikram Singh submitted evidence for verification.',
       'REMEDIATION_TASK',
       (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'DEMO:EVIDENCE_SUBMITTED:REM-0003',
       DATE_SUB(NOW(), INTERVAL 28 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'analyst2') AND dedup_key = 'DEMO:EVIDENCE_SUBMITTED:REM-0003');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'manager'),
       'EVIDENCE_SUBMITTED',
       'Evidence submitted for REM-0003',
       'Vikram Singh submitted evidence for verification.',
       'REMEDIATION_TASK',
       (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0003'),
       'DEMO:EVIDENCE_SUBMITTED:REM-0003',
       DATE_SUB(NOW(), INTERVAL 28 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'manager') AND dedup_key = 'DEMO:EVIDENCE_SUBMITTED:REM-0003');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'analyst'),
       'INCIDENT_ASSIGNED',
       'Incident INC-0001 assigned to you',
       'Malware infection on finance workstation.',
       'INCIDENT',
       (SELECT id FROM incidents WHERE incident_code = 'INC-0001'),
       'DEMO:INCIDENT_ASSIGNED:INC-0001',
       DATE_SUB(NOW(), INTERVAL 29 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'analyst') AND dedup_key = 'DEMO:INCIDENT_ASSIGNED:INC-0001');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'analyst'),
       'CRITICAL_VULNERABILITY',
       'Critical vulnerability on SRV-104',
       'VUL-0001 was found on Finance Production Server.',
       'ASSET_VULNERABILITY',
       (SELECT av.id FROM asset_vulnerabilities av JOIN assets a ON a.id = av.asset_id JOIN vulnerabilities v ON v.id = av.vulnerability_id WHERE a.asset_code = 'SRV-104' AND v.vuln_code = 'VUL-0001'),
       'DEMO:CRITICAL_VULNERABILITY:SRV-104',
       DATE_SUB(NOW(), INTERVAL 140 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'analyst') AND dedup_key = 'DEMO:CRITICAL_VULNERABILITY:SRV-104');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'auditor'),
       'AUDIT_EVIDENCE_SUBMITTED',
       'Audit evidence submitted for FND-0002',
       'Ananya Rao uploaded evidence for a corrective action.',
       'CORRECTIVE_ACTION',
       (SELECT id FROM corrective_actions WHERE finding_id = (SELECT id FROM audit_findings WHERE finding_code = 'FND-0002') AND description = 'Document the security log retention period and get it approved by the Security Manager.'),
       'DEMO:AUDIT_EVIDENCE:FND-0002',
       DATE_SUB(NOW(), INTERVAL 20 HOUR),
       NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'auditor') AND dedup_key = 'DEMO:AUDIT_EVIDENCE:FND-0002');

INSERT INTO notifications (recipient_id, category, title, message, record_type, record_id, dedup_key, created_at, read_at)
SELECT (SELECT id FROM users WHERE username = 'engineer2'),
       'EVIDENCE_APPROVED',
       'Evidence approved for REM-0004',
       'Your evidence was approved and the task is completed.',
       'REMEDIATION_TASK',
       (SELECT id FROM remediation_tasks WHERE task_code = 'REM-0004'),
       'DEMO:EVIDENCE_APPROVED:REM-0004',
       DATE_SUB(NOW(), INTERVAL 500 HOUR),
       DATE_SUB(NOW(), INTERVAL 480 HOUR)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_id = (SELECT id FROM users WHERE username = 'engineer2') AND dedup_key = 'DEMO:EVIDENCE_APPROVED:REM-0004');

-- ---------------------------------------------------------------------
-- Audit log entry recording that demo data was loaded
-- ---------------------------------------------------------------------
INSERT INTO audit_logs (user_id, username, action, module, record_type, record_id, description, ip_address, created_at)
SELECT NULL,
       'system',
       'SEED',
       'SYSTEM',
       NULL,
       NULL,
       'Demonstration data loaded by DemoDataInitializer (dev profile).',
       NULL,
       NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE action = 'SEED' AND module = 'SYSTEM');


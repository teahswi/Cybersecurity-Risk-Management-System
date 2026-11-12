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

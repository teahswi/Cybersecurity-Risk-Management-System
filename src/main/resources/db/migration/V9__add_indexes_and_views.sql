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

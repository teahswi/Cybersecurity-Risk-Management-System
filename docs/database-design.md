# CyberShield — Database Design

Database: **`cybershield_db`** (MySQL 8.x, InnoDB, `utf8mb4`). Schema source of truth:
`src/main/resources/db/migration/V1…V10` (Flyway). Hibernate only *validates* the mappings.

## 1. Design principles

* **Surrogate keys** – every table has `id BIGINT AUTO_INCREMENT PRIMARY KEY`. Human-readable business codes
  (`SRV-104`, `VUL-0001`, `REM-0007`, …) are separate `UNIQUE` columns and are never used as foreign keys.
  Generated codes are derived from the id right after the insert (`REM-` + zero-padded id) inside the same transaction.
* **Enums as strings** – status/type columns are `VARCHAR` storing the Java enum name
  (`@Enumerated(EnumType.STRING)`), each protected by a `CHECK (col IN (...))` constraint. Ordinals are never stored.
* **No physical deletion of history-bearing rows** – assets are archived (`status = 'ARCHIVED'`); users are deactivated.
  Foreign keys use the default `RESTRICT` behaviour so referenced rows cannot be deleted.
* **Append-only tables** – `remediation_history`, `remediation_evidence_reviews`, `audit_evidence_reviews`,
  `investigation_notes`, `incident_alerts`, `incident_assets` and `audit_logs` are only ever inserted into.
* **Dates** – `DATE` for calendar dates (deadlines, discovery dates), `DATETIME` for timestamps.
* **Money/precision** – `DECIMAL(3,1)` for CVSS scores (exact decimal, 0.0–10.0).

## 2. Tables

### Reference tables
| Table | Purpose | Key constraints |
|---|---|---|
| `roles` | The four application roles | `UNIQUE(code)`, `CHECK code IN (4 roles)` |
| `departments` | Company departments | `UNIQUE(code)`, `UNIQUE(name)` |
| `office_locations` | Hyderabad, Bengaluru, Pune offices | `UNIQUE(code)`, `UNIQUE(name)` |
| `compliance_frameworks` | ISO 27001 / NIST CSF (demonstration subsets) | `UNIQUE(code)` |

### Users
| Table | Purpose | Key constraints |
|---|---|---|
| `users` | Accounts. **Exactly one role** per user: `role_id → roles.id` (many-to-one) | `UNIQUE(username)`, `UNIQUE(email)`, `FK role_id`, `FK department_id`, `CHECK status IN ('ACTIVE','INACTIVE')`, `CHECK` password hash is 60 chars starting with `$2` (BCrypt) |

### Assets and software
| Table | Purpose | Key constraints |
|---|---|---|
| `assets` | IT assets | `UNIQUE(asset_code)`, FKs to department, office, owner (user); CHECKs on type, criticality, environment, status |
| `software` | Software catalogue with company **baseline version** | `UNIQUE(name, vendor)`, CHECK category |
| `asset_software` | **Junction (M:N assets ↔ software)** with the *installed version*, installation date and licence | `UNIQUE(asset_id, software_id)`, CHECK licence type, `CHECK license_expiry_date >= installation_date` |

### Vulnerabilities
| Table | Purpose | Key constraints |
|---|---|---|
| `vulnerabilities` | Vulnerability **definitions** | `UNIQUE(vuln_code)`, `UNIQUE(cve_id)` (NULL allowed – demo data has none), `CHECK cvss_score BETWEEN 0.0 AND 10.0`, CHECK severity |
| `asset_vulnerabilities` | **Junction (M:N assets ↔ vulnerabilities)** *and* an entity: one vulnerability on one asset with its own status and dates | `UNIQUE(asset_id, vulnerability_id)`, CHECK status, `CHECK last_observed_date >= discovered_date` |

### Remediation
| Table | Purpose | Key constraints |
|---|---|---|
| `remediation_tasks` | Task for **one** `asset_vulnerabilities` row | `UNIQUE(task_code)`, FKs, CHECK priority/status, `CHECK (status = 'OPEN' OR assigned_engineer_id IS NOT NULL)`, `CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))` |
| `remediation_history` | Append-only task history (actions, status before/after, assignee before/after, actor) | FKs to task and users, CHECK action_type |
| `remediation_evidence` | Uploaded evidence metadata (file stored on disk) | `UNIQUE(stored_filename)`, CHECK mime type list, `CHECK file_size BETWEEN 1 AND 10 MB`, CHECK status |
| `remediation_evidence_reviews` | Every approve/reject decision | FK evidence, FK reviewer, CHECK decision |

### Monitoring and incidents
| Table | Purpose | Key constraints |
|---|---|---|
| `security_alerts` | Recorded alerts (no live SIEM) | `UNIQUE(alert_code)`, CHECK source/severity/status |
| `incidents` | Investigations | `UNIQUE(incident_code)`, CHECKs, `CHECK status NOT IN ('RESOLVED','CLOSED') OR resolution_details IS NOT NULL` |
| `incident_alerts` | **Junction (M:N incidents ↔ alerts)** | `UNIQUE(incident_id, alert_id)` |
| `incident_assets` | **Junction (M:N incidents ↔ assets)** with impact description | `UNIQUE(incident_id, asset_id)` |
| `investigation_notes` | Notes on an incident (1:N) | FK incident, FK author |

### Audit and compliance
| Table | Purpose | Key constraints |
|---|---|---|
| `compliance_controls` | Controls of a framework (1:N from frameworks) | `UNIQUE(framework_id, control_code)` |
| `audit_plans` | Audit against one framework | `UNIQUE(plan_code)`, CHECK status, `CHECK target_completion_date >= start_date` |
| `audit_findings` | Findings of a plan (1:N), optionally against a control | `UNIQUE(finding_code)`, CHECK severity/status |
| `corrective_actions` | Actions for a finding (**1:N** – a finding may need several actions) | CHECK status |
| `audit_evidence` | Evidence for a corrective action (separate from remediation evidence) | `UNIQUE(stored_filename)`, CHECK mime/size/status |
| `audit_evidence_reviews` | Verify/reject decisions | CHECK decision |

### Risk
| Table | Purpose | Key constraints |
|---|---|---|
| `risk_assessments` | Risk register | `CHECK likelihood/impact BETWEEN 1 AND 5`, **`CHECK risk_score = likelihood * impact`**, CHECK that `risk_level` matches the score band, CHECK strategy/status |
| `risk_treatments` | Treatment actions for a risk (1:N) | CHECK strategy/status |

### System
| Table | Purpose | Key constraints |
|---|---|---|
| `notifications` | Per-user notifications | FK recipient, `UNIQUE(recipient_id, dedup_key)` (prevents duplicate reminders; NULL keys not deduplicated), CHECK category |
| `audit_logs` | Append-only activity log | `user_id` **nullable** (failed logins with unknown usernames), `username` keeps the attempted name |

## 3. Primary and foreign keys

Every table: `PRIMARY KEY (id)`. Foreign keys (all `ON DELETE RESTRICT`, the InnoDB default):

```
users.role_id → roles.id                        users.department_id → departments.id
assets.department_id → departments.id           assets.location_id → office_locations.id
assets.owner_id → users.id                      asset_software.(asset_id, software_id) → assets, software
vulnerabilities.created_by → users.id           asset_vulnerabilities.(asset_id, vulnerability_id) → assets, vulnerabilities
remediation_tasks.asset_vulnerability_id → asset_vulnerabilities.id
remediation_tasks.assigned_engineer_id / created_by → users.id
remediation_history.task_id → remediation_tasks.id; previous/new_assignee_id, performed_by_id → users.id
remediation_evidence.task_id → remediation_tasks.id; submitted_by_id → users.id
remediation_evidence_reviews.evidence_id → remediation_evidence.id; reviewer_id → users.id
security_alerts.asset_id → assets.id; assigned_analyst_id, created_by → users.id
incidents.assigned_analyst_id, created_by → users.id
incident_alerts.(incident_id, alert_id) → incidents, security_alerts
incident_assets.(incident_id, asset_id) → incidents, assets
investigation_notes.incident_id → incidents.id
compliance_controls.framework_id → compliance_frameworks.id
audit_plans.framework_id → compliance_frameworks.id; assigned_auditor_id → users.id
audit_findings.audit_plan_id → audit_plans.id; control_id → compliance_controls.id
corrective_actions.finding_id → audit_findings.id; assigned_to_id → users.id
audit_evidence.corrective_action_id → corrective_actions.id
audit_evidence_reviews.evidence_id → audit_evidence.id
risk_assessments.asset_id → assets.id; asset_vulnerability_id → asset_vulnerabilities.id; owner_id → users.id
risk_treatments.risk_id → risk_assessments.id
notifications.recipient_id → users.id           audit_logs.user_id → users.id (nullable)
```

InnoDB automatically indexes every foreign-key column.

## 4. Indexes (V9)

Secondary indexes support the search boxes, filters and dashboards: `assets(name, status, asset_type, environment,
ip_address, hostname)`, `users(status, full_name)`, `software(name)`, `vulnerabilities(severity, title)`,
`asset_vulnerabilities(status)`, `remediation_tasks(status, due_date, (assigned_engineer_id, status))`,
`remediation_history(task_id, created_at)`, `remediation_evidence(task_id, status)`,
`security_alerts(status, severity, detected_at)`, `incidents(status, detected_at)`, `audit_plans(status)`,
`audit_findings(status)`, `corrective_actions(status)`, `risk_assessments(risk_level, status)`,
`notifications((recipient_id, read_at), (recipient_id, created_at))`,
`audit_logs(created_at, (record_type, record_id), action, module)`.

The composite `notifications(recipient_id, read_at)` index serves the unread counter that every page shows.

## 5. Relationships

**One-to-many:** role → users, department → assets/users, office → assets, framework → controls,
audit plan → findings, finding → corrective actions, corrective action → audit evidence, task → history,
task → evidence, evidence → reviews, incident → notes, risk → treatments, user → notifications, user → audit logs.

**Many-to-many (junction tables):**

| Relationship | Junction | Extra data on the junction |
|---|---|---|
| assets ↔ software | `asset_software` | installed version, installation date, licence type and expiry |
| assets ↔ vulnerabilities | `asset_vulnerabilities` | status, discovered/last observed dates, notes |
| incidents ↔ alerts | `incident_alerts` | who linked it and when |
| incidents ↔ assets | `incident_assets` | impact description, who/when |

`asset_vulnerabilities` is the key design decision: a vulnerability **definition** (e.g. VUL-0001) can affect many
assets, but each affected asset has its own status. A remediation task references **one** `asset_vulnerabilities`
row, so approving the SRV-104 task marks only *SRV-104's* instance as `MITIGATED` – SRV-110, affected by the same
definition, is untouched. Risk assessments may also reference a specific instance.

## 6. Normalisation

* **1NF** – every column is atomic (e.g. installed software is one row per asset/product, not a comma list).
* **2NF** – junction tables keep only facts that depend on the whole pair: `installed_version` depends on
  (asset, software), so it lives in `asset_software`, not in `software`; `status` of a vulnerability depends on
  (asset, vulnerability), so it lives in `asset_vulnerabilities`.
* **3NF** – no transitive dependencies: department and office names are stored once in their own tables; the
  assigned engineer is stored on the task, not on the vulnerability; role names are stored in `roles`.
* **Deliberate derived column** – `risk_assessments.risk_score`/`risk_level` are derived from likelihood × impact.
  They are persisted for fast sorting/filtering/reporting, and the `CHECK` constraints guarantee they can never
  disagree with the formula (a controlled denormalisation).

## 7. Transactions

Every service method that changes data is `@Transactional`; read methods are `@Transactional(readOnly = true)`.
Example – approving remediation evidence is **one** transaction that:

1. inserts a `remediation_evidence_reviews` row for each submitted file and updates their status,
2. updates `remediation_tasks.status = 'COMPLETED'` and `completed_at`,
3. updates the related `asset_vulnerabilities.status = 'MITIGATED'`,
4. inserts a `remediation_history` row,
5. inserts an `audit_logs` row,
6. inserts `notifications` for the engineer and the task creator.

If any step fails (e.g. a CHECK constraint), all of them are rolled back. The integration tests run each test in a
transaction that is rolled back, so the test database is left unchanged.

## 8. SQL joins, aggregation and grouping used by the application

* **Joins** – e.g. the remediation list (`RemediationTaskRepository.search`) joins tasks → asset_vulnerabilities →
  assets and vulnerabilities, plus a `LEFT JOIN` to users for the (optional) engineer.
* **LEFT JOIN + GROUP BY** – assets per department / office (`DepartmentRepository.countAssetsPerDepartment`).
* **GROUP BY with COUNT** – dashboard charts: open findings per severity, incidents per status, assets per type,
  tasks per status, open risks per level; active users per role.
* **Conditional aggregation** – `SUM(CASE WHEN status IN ('OPEN','MITIGATING') THEN 1 ELSE 0 END)` for open
  instances per vulnerability.
* **Subqueries** – `EXISTS` for "vulnerabilities with an instance in status X"; `IN (SELECT …)` to restrict
  engineers to assets/vulnerabilities related to their tasks.
* **Views** (V9):
  * `v_asset_vulnerability_summary` – per asset: total findings, open findings, open critical findings, highest
    CVSS (LEFT JOINs + GROUP BY + conditional SUM + MAX). Used by the asset inventory report.
  * `v_remediation_overview` – a five-table join of tasks, instances, assets, vulnerabilities and engineers.

Example queries for a viva:

```sql
-- Open vulnerabilities per severity (dashboard chart)
SELECT v.severity, COUNT(*) AS open_findings
FROM asset_vulnerabilities av
JOIN vulnerabilities v ON v.id = av.vulnerability_id
JOIN assets a ON a.id = av.asset_id
WHERE av.status IN ('OPEN', 'MITIGATING') AND a.status <> 'ARCHIVED'
GROUP BY v.severity;

-- Remediation completion percentage
SELECT ROUND(100 * SUM(status = 'COMPLETED') / NULLIF(COUNT(*), 0)) AS completion_percent
FROM remediation_tasks;

-- Overdue tasks per engineer
SELECT u.full_name, COUNT(*) AS overdue
FROM remediation_tasks t JOIN users u ON u.id = t.assigned_engineer_id
WHERE t.due_date < CURDATE() AND t.status <> 'COMPLETED'
GROUP BY u.full_name;

-- Assets with their exposure (view)
SELECT * FROM v_asset_vulnerability_summary ORDER BY open_critical_findings DESC, highest_cvss DESC;
```

## 9. Audit logging

`audit_logs` records login success, login failure (with a `NULL` user when the username does not exist), logout,
and every create/update/archive/link/assign/status change/evidence action/report export/profile and user change,
with the acting user, IP address, module, record type and record id. The application:

* only inserts into it (`AuditLogRepository` extends the bare Spring Data `Repository` and declares only `save` and
  read queries; the entity is `@Immutable`),
* writes the log entry in the **same transaction** as the business change,
* never records passwords,
* uses it for the dashboard activity timeline, record history panels and the manager's audit log browser – there
  is no duplicate "activity" table.

`database/optional-append-only-triggers.sql` adds `BEFORE UPDATE/DELETE` triggers (`SIGNAL SQLSTATE '45000'`) for
database-level enforcement; it needs root because of MySQL's binary-logging rule for triggers.

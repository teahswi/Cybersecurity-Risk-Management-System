# CyberShield — Workflow Documentation

Each workflow below lists who does what, which status changes happen, and which database rows are written.
All multi-step actions are single `@Transactional` service methods.

## 1. Login workflow

1. User opens any page → Spring Security redirects to `/login`.
2. User submits username + password (with CSRF token) to `POST /login`.
3. `CustomUserDetailsService` loads the user **and its role** from MySQL (`users JOIN roles`).
4. `DaoAuthenticationProvider` checks the account is `ACTIVE` and compares the password with the BCrypt hash.
5. **Success** → `LoginSuccessHandler` updates `users.last_login_at`, inserts `audit_logs (LOGIN_SUCCESS)` and
   redirects to the requested page or `/dashboard`. The session holds a `SecurityUser` with authority
   `ROLE_<CODE>`.
6. **Failure** → `LoginFailureHandler` inserts `audit_logs (LOGIN_FAILURE)` (with `user_id = NULL` if the username
   does not exist, never the password) and redirects to `/login?error` or `/login?disabled`.
7. **Logout** (`POST /logout`) → session invalidated, cookie deleted, `audit_logs (LOGOUT)`.

## 2. Vulnerability discovery

1. Analyst/manager creates a **definition** (`vulnerabilities`) – title, description, severity, CVSS 0.0–10.0,
   optional CVE (validated format), recommended remediation. Code `VUL-xxxx` is generated from the id.
2. They **link** it to each affected asset (`asset_vulnerabilities`, status `OPEN`, discovery date).
   `UNIQUE(asset_id, vulnerability_id)` prevents duplicates; archived assets are rejected.
3. If the severity is **CRITICAL**, every active analyst and manager receives a `CRITICAL_VULNERABILITY`
   notification (deduplicated per instance).
4. Manual instance statuses (only when no remediation task is active, always with a reason):
   `OPEN → ACCEPTED / CLOSED`, `ACCEPTED → OPEN / CLOSED`, `MITIGATED → CLOSED / OPEN`, `CLOSED → OPEN`.
   `MITIGATING` and `MITIGATED` are set **only** by the remediation workflow.

## 3. Risk assessment

1. From an asset or an asset-vulnerability page, the analyst opens *Assess risk* (asset and instance pre-filled).
2. They choose **Likelihood (1–5)** and **Impact (1–5)**, a treatment strategy (Mitigate/Avoid/Transfer/Accept),
   owner and status. The page shows a live preview, but the form contains **no score field**.
3. `RiskService` calculates `score = likelihood × impact` and the level (1–4 Low, 5–9 Medium, 10–16 High,
   17–25 Critical) and stores both. MySQL `CHECK` constraints reject any row whose score/level disagree with the formula.
4. Treatments (with owner and deadline) can be added; starting one moves the risk to *In Treatment*. Every change is
   in the audit log and shown as the risk's history.

## 4. Remediation assignment

```
OPEN ──assign──► ASSIGNED ──start──► IN_PROGRESS ──submit──► PENDING_VERIFICATION ──approve──► COMPLETED
                                         ▲                          │                              │
                                         └────────── reject ────────┘                           reopen
                                         ▲                                                          ▼
                                         └───────────────────── start ◄──────────────────────── REOPENED
```

1. On an asset-vulnerability page the analyst/manager fills *Create remediation task*: title, required action,
   priority, engineer (optional) and deadline (required when an engineer is chosen; never in the past).
2. Rules: the instance must be `OPEN`/`MITIGATING`, the asset not archived, and no other active task may exist for
   the same instance.
3. One transaction inserts the task (`OPEN`) + `remediation_history (CREATED)` + audit log; if an engineer was chosen
   it also changes the task to `ASSIGNED`, writes `remediation_history (ASSIGNED, previous → new assignee)`, an
   audit log and a `TASK_ASSIGNED` notification for the engineer.
4. Re-assignment is allowed while the task is Open/Assigned/In Progress/Reopened (history action `REASSIGNED`).

## 5. Engineer workflow

1. Engineer logs in → **My Assigned Tasks** (`/remediation`, filtered on the server to `assigned_engineer_id = me`).
2. Opens the task: sees the asset (hostname, IP, OS, department, office), the vulnerability and the history.
3. **Start work** – `ASSIGNED/REOPENED → IN_PROGRESS`; the asset-vulnerability instance becomes `MITIGATING`;
   history `STATUS_CHANGED`.
4. **Add notes** – history `NOTE_ADDED` (append-only).
5. **Upload evidence** (only while In Progress): stored as `DRAFT`, history `EVIDENCE_UPLOADED`. Drafts can be removed
   before submission; submitted evidence can never be deleted.
6. Any attempt by another engineer to change the task is rejected with *Access denied* on the server.

## 6. Evidence submission

1. Engineer clicks **Submit evidence** (requires at least one draft file).
2. Transaction: drafts → `SUBMITTED`; task `IN_PROGRESS → PENDING_VERIFICATION`; history `EVIDENCE_SUBMITTED`;
   audit log; `EVIDENCE_SUBMITTED` notifications to the task creator and every active Security Manager.

Upload validation (`FileStorageService`): extension in {pdf, png, jpg, jpeg, txt}; declared MIME type must match;
file signature check (`%PDF-`, PNG header, JPEG `FFD8FF`, no NUL bytes for text); ≤ 10 MB; stored as
`<uuid>.<ext>` under `UPLOAD_DIR/remediation`; the path is re-checked on every read to prevent path traversal.

## 7. Evidence verification

1. A Security Analyst or Manager (not the submitter, never an engineer or auditor) opens the task, downloads the
   files (download is access-checked and audit-logged) and chooses **Approve** or **Reject**.
2. **Approve** (one transaction): a `remediation_evidence_reviews` row per file (decision APPROVED), evidence →
   `APPROVED`, task → `COMPLETED` with `completed_at`, **only this task's** `asset_vulnerabilities` row → `MITIGATED`,
   history `EVIDENCE_APPROVED`, audit log, notifications `EVIDENCE_APPROVED` (engineer) and `TASK_COMPLETED`
   (task creator, if someone else reviewed). Dashboard numbers change on the next page load because they are
   calculated from these rows.
3. **Reject** (reason required): review rows (REJECTED), evidence → `REJECTED` (kept), task → `IN_PROGRESS`,
   history `EVIDENCE_REJECTED`, `EVIDENCE_REJECTED` notification to the engineer, who uploads new evidence.
4. **Reopen** (analyst/manager, reason required): `COMPLETED → REOPENED`, instance back to `OPEN`, history
   `REOPENED`, `TASK_REOPENED` notification. The engineer then starts work again.

## 8. Incident investigation

1. Analyst records an alert (or uses a seeded one) and clicks **Create incident from alert**, or creates an incident
   directly. Code `INC-xxxx`.
2. Linking an alert inserts `incident_alerts` and moves a New/Investigating alert to **Escalated**.
3. Assigning an investigator (analyst or manager) moves `OPEN → INVESTIGATING` and sends `INCIDENT_ASSIGNED`.
4. Affected assets are linked through `incident_assets` with an impact description.
5. Any analyst/manager can add investigation notes; only the investigator, the creator or a manager can change the
   incident.
6. Status transitions are validated: `OPEN → INVESTIGATING → CONTAINED ⇄ INVESTIGATING → RESOLVED → CLOSED`
   (`RESOLVED → INVESTIGATING` re-opens). Resolving requires resolution details (also enforced by a DB CHECK).
7. The incident timeline merges `investigation_notes` and the incident's `audit_logs` entries in time order.

## 9. Audit workflow

1. Auditor (or manager) creates an **audit plan** for a framework, assigned to a Compliance Auditor, with start and
   target dates (`CHECK target >= start`). Status `PLANNED → IN_PROGRESS → COMPLETED` (or `CANCELLED`).
2. During the audit the assigned auditor records **findings**, optionally against a control of the same framework.
   Managers and the plan auditor receive `AUDIT_FINDING` notifications.
3. For each finding, one or more **corrective actions** are assigned to any active user with a deadline
   (`CORRECTIVE_ACTION_ASSIGNED` notification; finding → In Progress).
4. The responsible user opens *My Corrective Actions*, optionally starts it, and uploads **audit evidence**
   (separate table and folder from remediation evidence) → action `PENDING_REVIEW`, notification
   `AUDIT_EVIDENCE_SUBMITTED` to the plan's auditor.
5. The auditor or a manager (not the submitter) **verifies** (action `VERIFIED`; when every action of the finding is
   verified the finding becomes `RESOLVED`) or **rejects** with notes (action back to `IN_PROGRESS`). The submitter
   gets `AUDIT_EVIDENCE_REVIEWED`. All reviews are kept in `audit_evidence_reviews`.
6. The *Audit findings report* (PDF/CSV) summarises findings, controls, status and unverified actions per plan.

## 10. Notification workflow

| Event | Recipients | Category |
|---|---|---|
| Critical vulnerability linked to an asset | all active analysts + managers | `CRITICAL_VULNERABILITY` |
| Alert assigned | assigned analyst | `ALERT_ASSIGNED` |
| Incident assigned | assigned analyst | `INCIDENT_ASSIGNED` |
| Remediation task assigned | assigned engineer | `TASK_ASSIGNED` |
| Deadline within 2 days | assigned engineer | `TASK_DEADLINE` |
| Task overdue | assigned engineer + managers | `TASK_OVERDUE` |
| Evidence submitted | task creator + managers | `EVIDENCE_SUBMITTED` |
| Evidence approved / rejected | assigned engineer | `EVIDENCE_APPROVED` / `EVIDENCE_REJECTED` |
| Task completed | task creator (if not the reviewer) | `TASK_COMPLETED` |
| Task reopened | assigned engineer | `TASK_REOPENED` |
| New audit finding | managers + plan auditor | `AUDIT_FINDING` |
| Corrective action assigned | responsible user | `CORRECTIVE_ACTION_ASSIGNED` |
| Audit evidence submitted | plan auditor | `AUDIT_EVIDENCE_SUBMITTED` |
| Audit evidence decision | submitter | `AUDIT_EVIDENCE_REVIEWED` |

Mechanics:

* Notifications are rows in `notifications`, created inside the same transaction as the event.
* `DeadlineReminderService` runs every hour via `@Scheduled` (first run 20 s after start-up). Its deduplication key
  contains the task id **and** the due date (`TASK_OVERDUE:42:2026-11-03`), and `UNIQUE(recipient_id, dedup_key)`
  guarantees no duplicates even after restarts; a new deadline produces a new reminder.
* Inactive users never receive notifications.
* The navbar bell polls `GET /api/notifications/summary` every 30 seconds (unread count + 8 latest). Clicking an item
  calls `POST /api/notifications/{id}/read` with the CSRF header and opens the related record.
* Users can only read or change their own notifications (`AccessDeniedException` otherwise).

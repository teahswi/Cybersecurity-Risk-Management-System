# CyberShield — Project Documentation

## 1. Problem statement

A medium-sized IT services company (about 1000 employees, three offices, cloud infrastructure, hundreds of servers
and endpoints) tracks its security work in spreadsheets, e-mail threads and separate tools. Nobody can answer simple
questions reliably: *Which servers still have a critical vulnerability? Who is fixing it, and by when? Was the fix
verified? Which audit findings are still open? What are our biggest risks?* Evidence of remediation is scattered,
history is overwritten, and access is not controlled by role.

## 2. Objectives

1. One centralised, role-based web application for assets, software, vulnerabilities, alerts, incidents,
   remediation, audits, compliance controls, risks, users, notifications and activity history.
2. A properly normalised MySQL database that demonstrates keys, constraints, one-to-many and many-to-many
   relationships, junction tables, views, indexes, transactions, joins and aggregation.
3. A verifiable remediation workflow: assignment → work → evidence → independent review → completion, with a
   permanent history.
4. Server-side enforcement of every permission, and an append-only audit trail.
5. A modern, accessible UI with light and dark themes, dashboards, search and exportable reports.

## 3. Limitations of the existing (manual) system

| Existing practice | Problem |
|---|---|
| Asset lists in spreadsheets | Duplicates, no unique asset codes, no link to vulnerabilities or incidents |
| Vulnerabilities tracked per scan report | A finding on 5 servers is one line — impossible to track each server's fix |
| Fixes reported by e-mail | No evidence, no reviewer, no deadline tracking, no history |
| Audit findings in documents | Corrective actions and evidence get lost; no status overview |
| Risk ratings typed by hand | Inconsistent scores; nobody knows how they were calculated |
| Shared admin accounts | No accountability, no audit trail |

## 4. Proposed system

CyberShield replaces these practices with a single Spring Boot application backed by MySQL:

* every record has a surrogate key and a human-readable code;
* a vulnerability on a specific asset is its own record, so each server's remediation is tracked separately;
* remediation is a validated state machine with evidence files and reviews;
* risk scores are calculated by the server and constrained by the database;
* every action is written to an append-only audit log and relevant users are notified in-app.

## 5. System architecture

```
 Browser (HTML5, Bootstrap 5.3, Thymeleaf-rendered pages, vanilla JS, Chart.js)
        │  HTTP (session cookie + CSRF token)
        ▼
 Spring Security filter chain ── form login, BCrypt, URL rules, CSRF, login/logout audit handlers
        ▼
 Spring MVC controllers (pages + small JSON API: charts, search, notifications)
        │  DTOs (validated forms in, read-only view records out)
        ▼
 Service layer ── business rules, @PreAuthorize, ownership checks, @Transactional, audit log, notifications
        ▼
 Spring Data JPA repositories (JPQL with joins, GROUP BY, subqueries) + JdbcTemplate for views
        ▼
 Hibernate (ddl-auto=validate) ──► MySQL 8 (schema created by Flyway V1–V10)

 Local disk (UPLOAD_DIR) ◄── FileStorageService (evidence files, outside the public web root)
 @Scheduled DeadlineReminderService ── hourly deadline / overdue notifications
```

Design rules followed: controllers contain no business logic; constructor injection everywhere; entities are never
returned from REST endpoints (records/DTOs only); `spring.jpa.open-in-view=false`, so views are built inside
read-only transactions; unidirectional `@ManyToOne` associations only (no recursive serialisation); pagination for
every large list.

## 6. Technology stack

Java 21 · Spring Boot 4.1.2 · Spring MVC · Spring Security 7 · Spring Data JPA / Hibernate 7 · Jakarta Validation ·
Flyway + flyway-mysql · MySQL 8 + Connector/J · Thymeleaf · Bootstrap 5.3.8 · Bootstrap Icons 1.13.2 ·
Chart.js 4.6.1 · Apache PDFBox 3.0.7 · Maven Wrapper · JUnit 5 / AssertJ / Spring Boot Test / Spring Security Test.

## 7. Modules

| Module | Main classes | Pages |
|---|---|---|
| Authentication & profile | `SecurityConfig`, `CustomUserDetailsService`, `LoginSuccessHandler`, `LoginFailureHandler`, `AuditLogoutSuccessHandler`, `UserService` | login, My Profile, access denied |
| Dashboard | `DashboardService`, `DashboardApiController` | dashboard |
| Assets | `AssetService`, `AssetController` | list, details, form |
| Software | `SoftwareService`, `SoftwareController` | list, details, form |
| Vulnerabilities | `VulnerabilityService`, `VulnerabilityController` | list, details, form, asset-vulnerability instance |
| Remediation | `RemediationService`, `RemediationController`, `FileStorageService`, `DeadlineReminderService` | task list / My Assigned Tasks, task details |
| Alerts | `AlertService`, `AlertController` | list, details, form |
| Incidents | `IncidentService`, `IncidentController` | list, details, form |
| Audit & compliance | `AuditService`, `AuditController` | audit plans, plan details/form, frameworks, controls, findings, finding details, corrective actions, action details |
| Risk | `RiskService`, `RiskController` | risk register (with 5×5 matrix), details, form |
| Notifications | `NotificationService`, `NotificationController`, `NotificationApiController` | navbar bell, notifications page |
| Users & reference data | `UserService`, `ReferenceAdminService`, `UserController`, `SettingsController` | users list/details/form, departments & offices |
| Search | `SearchService`, `SearchApiController`, `SearchController` | navbar search, results page |
| Reports | `ReportService`, `PdfReportWriter`, `CsvReportWriter`, `ReportController` | reports |
| System | `SystemService`, `AuditLogService`, `SystemController` | system health, audit logs |
| Errors | `GlobalExceptionHandler`, `ApiExceptionHandler` | 403, 404, 500, generic error |

## 8. Role permissions

See README section 16 for the full matrix. In short:

* **Security Analyst** – vulnerabilities, alerts, incidents, risk, creates/assigns/reviews remediation tasks.
* **IT Infrastructure Engineer** – only their own tasks: start, notes, evidence, submit; sees only related assets
  and vulnerabilities.
* **Compliance Auditor** – audit plans, controls, findings, corrective actions, audit evidence verification; read
  access to assets, software and risks.
* **Security Manager** – everything above (except doing an engineer's work), plus users, departments/offices,
  system health and audit logs. The last active manager can never be removed.

## 9. Security architecture

| Concern | Implementation |
|---|---|
| Authentication | Spring Security form login against the `users` table (`CustomUserDetailsService`); session-based (no JWT); `INACTIVE` accounts get a `DisabledException` |
| Passwords | `BCryptPasswordEncoder`; DB `CHECK` requires a 60-character `$2…` hash; strength policy (8+ chars, upper, lower, digit, symbol); change requires the current password |
| Authorisation layer 1 | URL rules in `SecurityFilterChain` (e.g. `/users/**` manager only) |
| Authorisation layer 2 | `@EnableMethodSecurity` + `@PreAuthorize` on every service operation |
| Authorisation layer 3 | Object-level checks in services: engineer = assignee, reviewer ≠ submitter, manager/auditor plan ownership, own notifications only |
| CSRF | Enabled; Thymeleaf adds the token to every form; `app.js` sends `X-CSRF-TOKEN` on AJAX POSTs; upload forms also carry it in the URL so oversized files still get a friendly error |
| Session | Logout invalidates the session and deletes `JSESSIONID`; 30-minute timeout; HttpOnly cookie |
| Uploads | Allow-list of extensions, declared content type must match, magic-byte signature check, 10 MB limit (server + DB `CHECK`), random UUID file names, path-traversal checks, files outside the static folder, access-controlled downloads with `Content-Disposition: attachment` and `nosniff` |
| Errors | `@ControllerAdvice` shows friendly pages; `server.error.include-stacktrace=never`; SQL/stack traces are only logged |
| Output encoding | Thymeleaf escapes all `th:text`; the search/notification JavaScript escapes HTML |
| CSV export | Formula-injection protection (`=`, `+`, `-`, `@` prefixed with `'`) |
| Audit | Append-only `audit_logs` for logins (success/failure), logouts and every business change; passwords are never logged |
| Secrets | Credentials only via environment variables; no passwords in version-controlled files |
| Clickjacking | `X-Frame-Options: DENY` |

## 10. DBMS concepts demonstrated

* Relational schema with 29 tables, BIGINT surrogate keys and business keys.
* Primary, foreign, UNIQUE (single and composite), NOT NULL and CHECK constraints — including cross-column checks
  (`risk_score = likelihood * impact`, task status requires an engineer, resolved incidents need resolution details,
  date ordering checks).
* One-to-many and many-to-many relationships; junction tables carrying attributes.
* Normalisation to 3NF with one documented, constraint-protected derived column.
* Secondary and composite indexes for search, filters and dashboards.
* Views with multi-table joins, `GROUP BY`, `COUNT`, `SUM(CASE …)`, `MAX`.
* Transactions (ACID) around multi-step workflow actions; rollback-based test isolation.
* Versioned schema migrations (Flyway) and schema validation (Hibernate).
* Append-only history tables and optional triggers (`SIGNAL SQLSTATE`).
* Idempotent seed scripts using `INSERT … SELECT … WHERE NOT EXISTS`.

## 11. Future scope

* Import vulnerability scan results (e.g. CSV from a scanner) to create `asset_vulnerabilities` automatically.
* Real SIEM / EDR integration through a message queue instead of manually recorded alerts.
* E-mail or chat notifications in addition to in-app notifications.
* Multi-factor authentication and account lockout after repeated failed logins.
* SLA policies per severity (e.g. critical = 15 days) with automatic deadline calculation.
* Evidence storage in object storage (S3-compatible) with virus scanning.
* Fine-grained permissions (several roles per user) if the organisation needs it.
* Trend charts (vulnerabilities opened vs. closed per month) using time-series aggregation.

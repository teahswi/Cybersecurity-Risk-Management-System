# CyberShield — Cybersecurity Management Platform

A full-stack college DBMS project: a database-driven cybersecurity management platform for a medium-sized
IT services company (≈1000 employees, three offices, hundreds of servers and endpoints).

Built with **Java 21, Spring Boot 4.1.2, Spring MVC, Spring Data JPA (Hibernate), Spring Security,
Thymeleaf, Bootstrap 5.3, Chart.js and MySQL 8**, with **Flyway** owning the database schema.

> Before you start, read [`BUILD-TEST-REPORT.md`](BUILD-TEST-REPORT.md). It states exactly what was verified
> while the project was built and what you must verify on your own machine (first `mvnw.cmd clean verify`).

---

## Table of contents
1. [Project overview](#1-project-overview)
2. [Project objectives](#2-project-objectives)
3. [Technologies used](#3-technologies-used)
4. [IntelliJ IDEA project configuration](#4-intellij-idea-project-configuration)
5. [Java 21 setup](#5-java-21-setup)
6. [Spring Boot 4.1.2 setup](#6-spring-boot-412-setup)
7. [Maven Wrapper](#7-maven-wrapper)
8. [MySQL Workbench setup](#8-mysql-workbench-setup)
9. [Database creation](#9-database-creation)
10. [Database user configuration](#10-database-user-configuration)
11. [Environment variables](#11-environment-variables)
12. [Flyway migration process](#12-flyway-migration-process)
13. [Running the application](#13-running-the-application)
14. [Application URL](#14-application-url)
15. [Demo accounts](#15-demo-accounts)
16. [Role permissions](#16-role-permissions)
17. [Modules](#17-modules)
18. [End-to-end demonstration guide (SRV-104)](#18-end-to-end-demonstration-guide-srv-104)
19. [Dark / light mode](#19-dark--light-mode)
20. [Reports and exports](#20-reports-and-exports)
21. [Running the tests](#21-running-the-tests)
22. [Troubleshooting](#22-troubleshooting)
23. [Project structure](#23-project-structure)

---

## 1. Project overview

CyberShield centralises the company's security operations in one web application:

| Area | What it manages |
|---|---|
| Inventory | IT assets (servers, endpoints, network devices, cloud VMs) and installed software versions |
| Vulnerabilities | Vulnerability definitions and **each vulnerability on each asset** (`asset_vulnerabilities`) |
| Remediation | A strictly enforced task workflow with evidence upload, review, history and notifications |
| Monitoring | Security alerts (stored records — **no live SIEM is connected**) and incident investigations |
| Compliance | Audit plans, ISO 27001 / NIST CSF demonstration controls, findings, corrective actions, audit evidence |
| Risk | Risk register with server-side scoring: **Risk Score = Likelihood × Impact** |
| Administration | Users and roles, departments and offices, system health, append-only audit logs |
| Insight | Role-aware dashboard and charts, global search, CSV/PDF reports, in-app notifications |

Everything shown on screen comes from MySQL — counts, charts, lists and reports are calculated with SQL
(`COUNT`, `GROUP BY`, joins and views) every time a page loads.

## 2. Project objectives

* Design a **normalised relational schema** (29 tables, BIGINT surrogate keys, business codes, foreign keys,
  UNIQUE and CHECK constraints, indexes, views) and evolve it with versioned migrations.
* Demonstrate **one-to-many** and **many-to-many** relationships, including junction tables that carry their own
  data (`asset_software.installed_version`, `asset_vulnerabilities.status`).
* Use **transactions** so that multi-step business actions (e.g. approving evidence) either fully succeed or
  fully roll back.
* Keep **append-only history** (remediation history, evidence reviews, audit logs).
* Enforce **role-based security on the server**, not just by hiding buttons.
* Deliver a polished, responsive UI with light and dark themes.

## 3. Technologies used

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 (Spring Framework 7, Spring Security 7, Spring Data JPA, Hibernate 7) |
| Web | Spring MVC + Thymeleaf (server-rendered pages) + a few small JSON endpoints |
| Database | MySQL 8.x, MySQL Connector/J, Flyway (+ `flyway-mysql`) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Frontend | HTML5, CSS3, Bootstrap 5.3.8, Bootstrap Icons 1.13.2, Chart.js 4.6.1, vanilla JavaScript (all served locally from `static/vendor`, no CDN needed) |
| Reports | Apache PDFBox 3.0.7 (PDF) and a hand-written RFC 4180 CSV writer |
| Build | Maven via the Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Tests | JUnit 5, AssertJ, Spring Boot Test, Spring Security Test, MockMvc, real MySQL test database |

No Lombok, no JWT, no Node.js, no external services. Dependency versions are managed by the Spring Boot
parent POM; only PDFBox (not managed by Boot) is pinned.

## 4. IntelliJ IDEA project configuration

The project was set up with the Spring Initializr values below and opens directly in IntelliJ IDEA:

| Setting | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.2 |
| Group / Artifact | `com.cybershield` / `cybershield` |
| Name | CyberShield |
| Package name | `com.cybershield` |
| Packaging | JAR |
| Java | 21 |

Steps:

1. **Install JDK 21** (see section 5).
2. **Open IntelliJ IDEA** → *File → Open…* → select the extracted **`CyberShield`** folder (the one that contains `pom.xml`).
3. Choose **Open as Project** / **Trust Project**. IntelliJ detects `pom.xml`; let Maven **import all dependencies**
   (the first import downloads them, which needs internet access).
4. *File → Project Structure → Project*: set **SDK = 21** and **Language level = 21**.
5. *Settings → Build, Execution, Deployment → Build Tools → Maven*:
   * **Maven home path**: *Use Maven wrapper* (recommended).
   * *Importing → JDK for importer*: **Project SDK (21)**.
   * *Runner → JRE*: **Project SDK (21)**.

## 5. Java 21 setup

Install any JDK 21 distribution (Eclipse Temurin, Oracle JDK, Microsoft Build of OpenJDK, or IntelliJ's
*Download JDK…* option). Check it in a terminal:

```cmd
java -version
```

It must print version `21.x`. If `mvnw.cmd` complains about `JAVA_HOME`, set it to the JDK 21 folder
(e.g. `C:\Program Files\Eclipse Adoptium\jdk-21...`) in *Windows → Environment Variables*.

## 6. Spring Boot 4.1.2 setup

`pom.xml` inherits from `spring-boot-starter-parent` **4.1.2**, so every Spring, Hibernate, Flyway, MySQL driver,
Thymeleaf and test library version is chosen by Spring Boot. Starters used (Spring Boot 4 names):

`spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`,
`spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-flyway`,
`org.flywaydb:flyway-mysql`, `com.mysql:mysql-connector-j`, `spring-boot-devtools` (optional),
`spring-boot-starter-test`, `spring-boot-starter-security-test`, and `org.apache.pdfbox:pdfbox:3.0.7`.

All code uses `jakarta.*` packages (no `javax.*`).

## 7. Maven Wrapper

You do **not** need to install Maven. The wrapper downloads Maven 3.9.11 the first time it runs:

```cmd
mvnw.cmd -v                 :: Windows
./mvnw -v                   # macOS / Linux / Git Bash
```

Configuration lives in `.mvn/wrapper/maven-wrapper.properties` (script-only wrapper, no binary JAR).

## 8. MySQL Workbench setup

1. Install **MySQL Server 8.x** and **MySQL Workbench** (MySQL Installer for Windows installs both).
2. Start the server: Workbench → *Server → Startup/Shutdown → Start Server*
   (or Windows *Services* → `MySQL80` → Start).
3. Connect to *Local instance MySQL80* as `root`.

## 9. Database creation

Open a new SQL tab in Workbench and execute:

```sql
CREATE DATABASE cybershield_db;
CREATE DATABASE cybershield_test;
```

* `cybershield_db` — the application database (development).
* `cybershield_test` — used **only** by the automated tests. Tests refuse to run against any other database.

You do **not** create tables by hand: Flyway creates them when the application starts (section 12).

## 10. Database user configuration

In the same SQL tab:

```sql
CREATE USER 'cybershield_user'@'localhost'
IDENTIFIED BY 'ChangeThisLocalPassword!';

GRANT ALL PRIVILEGES ON cybershield_db.*
TO 'cybershield_user'@'localhost';

GRANT ALL PRIVILEGES ON cybershield_test.*
TO 'cybershield_user'@'localhost';

FLUSH PRIVILEGES;
```

> `ChangeThisLocalPassword!` is only a **local development example**. Choose your own password and use the same
> value in `DB_PASSWORD` below. Real passwords are never stored in the project files.

## 11. Environment variables

The application reads credentials from environment variables (with safe defaults for everything except the password).

| Variable | Example value | Used for |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cybershield_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | application database |
| `DB_USERNAME` | `cybershield_user` | application database |
| `DB_PASSWORD` | *the password you chose in section 10* | application database |
| `UPLOAD_DIR` | `./uploads` | where evidence files are stored (outside the public static folder) |
| `TEST_DB_URL` *(optional)* | `jdbc:mysql://localhost:3306/cybershield_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | tests (this is the default) |
| `TEST_DB_USERNAME` / `TEST_DB_PASSWORD` *(optional)* | | tests — default to `DB_USERNAME` / `DB_PASSWORD` |

`allowPublicKeyRetrieval=true` is needed by MySQL 8's default `caching_sha2_password` authentication when SSL is
disabled on a local machine.

**IntelliJ Run Configuration:** *Run → Edit Configurations… → + → Spring Boot* (or *Application*):

* Main class: `com.cybershield.CyberShieldApplication`
* Active profiles: `dev`
* Environment variables:
  `DB_URL=jdbc:mysql://localhost:3306/cybershield_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true;DB_USERNAME=cybershield_user;DB_PASSWORD=your_password;UPLOAD_DIR=./uploads`

## 12. Flyway migration process

Flyway is the **only** component that creates or changes tables. On start-up it applies, in order, the scripts in
`src/main/resources/db/migration`:

| Version | Script |
|---|---|
| V1 | roles, departments, office locations, compliance frameworks, users |
| V2 | assets, software, asset_software |
| V3 | vulnerabilities, asset_vulnerabilities |
| V4 | remediation tasks, history, evidence, evidence reviews |
| V5 | security alerts, incidents, incident_alerts, incident_assets, investigation notes |
| V6 | compliance controls, audit plans, findings, corrective actions, audit evidence and reviews |
| V7 | risk assessments and treatments |
| V8 | notifications and audit logs |
| V9 | secondary indexes and two reporting views |
| V10 | reference data (4 roles, 8 departments, 3 offices, 2 frameworks, 19 controls) |

Flyway records what it applied in the `flyway_schema_history` table. Hibernate then **validates** the entity
mappings against the tables (`spring.jpa.hibernate.ddl-auto=validate`) — it never alters the schema.

`database/schema.sql` and `database/sample-data.sql` are **reference copies** generated from the same migration
and seed design (for reading or loading into a separate empty database). **Do not run them on `cybershield_db`
after the application has started** — the tables already exist.

`database/optional-append-only-triggers.sql` adds database-level triggers that block `UPDATE`/`DELETE` on the
append-only tables. It must be run **as root** (MySQL requires the SUPER privilege for triggers when binary
logging is on, which is the MySQL 8 default), so it is optional and not part of Flyway.

## 13. Running the application

**From IntelliJ:** run the `CyberShieldApplication` configuration from section 11.

**From a terminal** (Windows `cmd`, in the `CyberShield` folder):

```cmd
set DB_PASSWORD=your_password
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

PowerShell: `$env:DB_PASSWORD="your_password"; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`

On the first start you will see Flyway apply V1–V10, then
`Demo data check complete: 6 demo user(s) created, sample records ensured.`
Later starts create nothing new (the demo loader is idempotent and never overwrites existing users).

`dev` is also the default profile when none is given. The demo data loader runs **only** in the `dev` profile;
start a production deployment with another profile, e.g. `--spring.profiles.active=prod`.

## 14. Application URL

Open **http://localhost:8080** — you are redirected to the login page.

## 15. Demo accounts

| Role | Username | Password |
|---|---|---|
| Security Analyst | `analyst` | `Analyst@123` |
| IT Infrastructure Engineer | `engineer` | `Engineer@123` |
| Compliance Auditor | `auditor` | `Auditor@123` |
| Security Manager | `manager` | `Manager@123` |
| Security Analyst (second account) | `analyst2` | `Demo@12345` |
| IT Infrastructure Engineer (second account) | `engineer2` | `Demo@12345` |

These are **development demonstration credentials only**. They are inserted with BCrypt-hashed passwords by
`DemoDataInitializer` (dev profile). The two extra accounts exist so you can demonstrate that one engineer cannot
touch another engineer's tasks, and that alerts/incidents can be assigned to different analysts.

## 16. Role permissions

There are exactly four roles; each user has exactly one (`users.role_id → roles.id`). Database code
`SECURITY_MANAGER` ↔ Spring authority `ROLE_SECURITY_MANAGER` ↔ `hasRole('SECURITY_MANAGER')`.

| Capability | Analyst | Engineer | Auditor | Manager |
|---|:-:|:-:|:-:|:-:|
| View assets | ✔ all | ✔ own / task-related only | ✔ all | ✔ all |
| Add / edit / archive assets, software, installations | | | | ✔ |
| View software inventory | ✔ | | ✔ | ✔ |
| Create / edit vulnerabilities, link to assets | ✔ | | | ✔ |
| View vulnerabilities | ✔ | ✔ task-related only | | ✔ |
| Create, assign, review, reopen remediation tasks | ✔ | | | ✔ |
| Start work, add notes, upload & submit evidence | | ✔ own tasks only | | |
| Alerts and incidents | ✔ | | | ✔ |
| Risk register | ✔ edit | | view | ✔ edit |
| Audit plans, controls, findings, corrective actions, verify audit evidence | | | ✔ | ✔ |
| Work on corrective actions assigned to me | ✔ | ✔ | ✔ | ✔ |
| Users, departments, offices, system health, audit logs | | | | ✔ |
| Reports | asset, vulnerability, incident, remediation, risk | own remediation | asset, audit, risk | all |

Additional rules enforced in the service layer (every rule is checked on the backend):

* Nobody can change their own role or deactivate their own account; the **last active Security Manager** can never
  be deactivated or demoted.
* Engineers cannot modify tasks assigned to someone else and can never approve evidence.
* A reviewer cannot approve evidence they submitted; an auditor's audit-evidence permission gives **no** access to
  remediation evidence.
* Audit logs have no update/delete operation anywhere in the application.
* CSRF protection is enabled for every form and AJAX call.

## 17. Modules

| Module | Highlights |
|---|---|
| **Login & profile** | Database-backed login (BCrypt), inactive-account handling, login/logout/failed-login audit, My Profile, change password (current password required). |
| **Dashboard** | Role-aware summary cards, 5 Chart.js charts from `/api/dashboard/charts`, remediation completion %, deadlines, critical vulnerabilities, pending reviews, recent incidents, major risks, active audits, activity timeline (from `audit_logs`), notifications, quick actions. |
| **Assets** | Search by code/name/hostname/IP, filters (department, office, type, environment, status), sortable columns, pagination, details page with software, vulnerabilities, remediation history, incidents, alerts, risks and change history; archive / restore instead of delete. |
| **Software** | Catalogue with company baseline versions; installations per asset with licence data; outdated versions flagged by numeric version comparison. |
| **Vulnerabilities** | Definitions (CVSS 0.0–10.0, optional CVE — demo data uses none), affected assets, per-asset instance page with manual status rules, risk assessment and task creation. |
| **Remediation** | State machine OPEN → ASSIGNED → IN_PROGRESS → PENDING_VERIFICATION → COMPLETED (+ rejection and REOPENED), engineer notes, evidence upload (PDF/PNG/JPG/TXT, 10 MB, signature checks, UUID file names), approval/rejection with preserved review history, append-only task history. |
| **Alerts** | Recorded alerts with validated status transitions, analyst assignment, history, "create incident from alert". |
| **Incidents** | Investigator assignment, linked alerts and affected assets (many-to-many), notes, validated status changes, resolution details, timeline merged from notes and the audit log. |
| **Audit & compliance** | Frameworks, controls, audit plans, findings, corrective actions, audit evidence upload and verification; a finding becomes Resolved when all its actions are verified. |
| **Risk** | Likelihood × Impact scoring on the server, level bands, treatments with deadlines, 5×5 heat map, history. |
| **Notifications** | Per-user, database-backed, navbar bell with unread counter (polled every 30 s), dropdown, notifications page, mark read / mark all read, deduplication keys, hourly deadline/overdue job (`@Scheduled`). |
| **Global search** | Assets, vulnerabilities, incidents and tasks — filtered by role on the server. |
| **Reports** | Six reports as CSV or PDF with filters (section 20). |
| **Administration** | Users, departments & offices, system health (live `SELECT 1`), audit log browser. |

See `docs/project-documentation.md`, `docs/workflow-documentation.md`, `docs/database-design.md` and
`docs/ER-diagram.md` for details.

## 18. End-to-end demonstration guide (SRV-104)

The demo data contains **SRV-104 — Finance Production Server** (Finance, Production, High criticality) with the
sample vulnerability **VUL-0001 "[DEMO] Remote code execution in outdated web application framework"**
(CVSS 9.8, no real CVE). SRV-104 deliberately has **no remediation task yet**.

| # | Who | Do this | You should see |
|---|---|---|---|
| 1–2 | analyst | Log in → **Assets** | Asset inventory |
| 3–4 | analyst | Search `SRV-104` → **View** | Asset details page |
| 5 | analyst | Under *Linked vulnerabilities* click **Open** for VUL-0001 | Asset vulnerability page |
| 6 | analyst | **Assess risk** → likelihood 4, impact 5 → Save | Score 20, level Critical (calculated by the server) |
| 7–9 | analyst | Back on the instance page, fill *Create remediation task*: priority, engineer **Vikram Singh**, deadline → **Create task** | Task REM-xxxx in status **Assigned**; history shows Created + Assigned |
| 10–12 | engineer | Log out, log in as `engineer` → **My Assigned Tasks** → open the task | Bell shows a new "New remediation task" notification |
| 13 | engineer | **Start work (In Progress)** | Status In Progress; VUL-0001 on SRV-104 becomes Mitigating |
| 14 | engineer | Add a remediation note | Note appears in the history |
| 15 | engineer | Upload a `.txt`/`.pdf`/`.png` evidence file | Evidence listed as **Draft** |
| 16–17 | engineer | **Submit evidence** | Status **Pending Verification** |
| 18 | analyst / manager | Log in | Notification "Evidence submitted for REM-xxxx" |
| 19 | reviewer | Open the task → download the evidence | File downloads (access-controlled) |
| 20–22 | reviewer | **Approve** → Record decision | Task **Completed**; SRV-104's VUL-0001 instance **Mitigated** — SRV-110 with the same vulnerability is *not* changed |
| 23 | reviewer | (Alternative) **Reject** with a reason | Task back to **In Progress**; rejected evidence and review kept |
| 24 | anyone | Dashboard | Completion % and counts updated |
| 25 | engineer | Bell | "Evidence approved" notification |
| 26 | manager | **Audit Logs** | Every step logged with user, time and IP |
| 27–28 | anyone | Task page | Full ordered history, evidence and verification history |

The same scenario is automated in `RemediationWorkflowTest.completeSrv104RemediationWorkflow` and
`PageRenderingTest.srv104WorkflowThroughHttpEndpoints`.

Other seeded examples: REM-0003 is waiting for verification (log in as `manager` or `analyst2` to review it),
REM-0005 is overdue, REM-0004 is completed, AUD-0001 has findings and corrective actions, INC-0001 / INC-0002
have linked alerts and assets.

## 19. Dark / light mode

* Use the moon/sun button in the navbar (also on the login page).
* The first visit follows the device setting (`prefers-color-scheme`); your choice is stored in `localStorage`
  (`cs-theme`) and takes priority afterwards.
* A tiny script in `<head>` sets `data-bs-theme` **before** the stylesheets load, so there is no wrong-theme flash.
* The whole UI uses Bootstrap 5.3 colour modes plus CSS variables; Chart.js charts are re-drawn with theme colours
  when you switch.

## 20. Reports and exports

*Reports* lists only the reports your role may generate. Each has filters and **Download PDF** / **Download CSV**.

| Report | Filters | Roles |
|---|---|---|
| Asset inventory (uses the `v_asset_vulnerability_summary` view) | department, asset status | analyst, auditor, manager |
| Vulnerabilities (per asset instance) | severity, status | analyst, manager |
| Incidents | status, detected from/to | analyst, manager |
| Remediation | task status (engineers: own tasks only) | analyst, engineer, manager |
| Audit findings | audit plan | auditor, manager |
| Risk register | level, status | analyst, auditor, manager |

PDFs (Apache PDFBox) are landscape A4 with title, generation date, user, filters, the database records and
"Page X of Y". CSV files are UTF-8 (opens in Excel) and protected against spreadsheet formula injection.
Every export is recorded in the audit log.

## 21. Running the tests

1. Make sure `cybershield_test` exists and `cybershield_user` has privileges on it (sections 9–10).
2. Set `DB_PASSWORD` (or `TEST_DB_PASSWORD`) in the terminal.
3. Run:

```cmd
set DB_PASSWORD=your_password
mvnw.cmd clean verify
```

The test profile (`application-test.properties`) points at `cybershield_test`; Flyway migrates it automatically,
the demo data is loaded once, and **every test runs in a transaction that is rolled back**. A guard in
`IntegrationTestBase` fails immediately if the connection is not `cybershield_test`, so tests can never modify
`cybershield_db`. Uploaded test files go to the system temp folder.

Test classes: `TaskStatusTest`, `RiskScoreTest`, `UtilitiesTest`, `FileStorageServiceTest`, `ReportWriterTest`
(unit) and `ApplicationAndDatabaseTest`, `AuthenticationAndAuthorizationTest`, `AssetAndVulnerabilityTest`,
`RemediationWorkflowTest`, `NotificationIncidentAuditRiskTest`, `ReportDashboardSearchTest`, `PageRenderingTest`
(integration, MySQL). `PageRenderingTest` renders every page for every role, so template problems fail the build.

## 22. Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'cybershield_user'@'localhost'` | `DB_PASSWORD` does not match the MySQL user's password, or the variable is not set in the Run Configuration. |
| `Public Key Retrieval is not allowed` | Add `allowPublicKeyRetrieval=true` to `DB_URL` (the default URL already has it). |
| `Unknown database 'cybershield_db'` | Run the `CREATE DATABASE` statements (section 9). |
| `Communications link failure` | MySQL server is not running, or not on port 3306. |
| `Schema-validation: missing table ...` | Flyway did not run or the database was created by hand. Use an empty database and let Flyway create it; check `spring.flyway.enabled=true`. |
| `Validate failed: Migrations have failed validation` (Flyway) | A migration file was edited after it was applied. In development: `DROP DATABASE cybershield_db; CREATE DATABASE cybershield_db;` and restart. Never edit applied migrations — add a new `V11__...sql`. |
| Ran `database/schema.sql` on cybershield_db by mistake | Drop and recreate `cybershield_db`, then start the application (Flyway rebuilds it). |
| Port 8080 already in use | Stop the other program or start with `--server.port=8081`. |
| `JAVA_HOME is not defined correctly` / wrong Java | Point `JAVA_HOME` to JDK 21; set IntelliJ Project SDK and Maven runner JRE to 21. |
| Maven cannot download dependencies | Check internet/proxy access to `repo.maven.apache.org`; in IntelliJ use *Maven → Reload All Maven Projects*. |
| Demo users missing | The `dev` profile was not active. Use `-Dspring-boot.run.profiles=dev` or Active profiles = `dev`. |
| Upload says "larger than the 10 MB limit" | Evidence files are limited to 10 MB (PDF, PNG, JPG/JPEG, TXT). |
| "This account is inactive" | A Security Manager must reactivate the account under **Users**. |
| Tests fail with "must only run against the cybershield_test database" | `TEST_DB_URL` points at the wrong database. |
| Error creating triggers (1419) | Run `optional-append-only-triggers.sql` as **root** (it is optional). |

## 23. Project structure

```
CyberShield/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/       Maven + wrapper
├── README.md, BUILD-TEST-REPORT.md
├── database/   schema.sql, sample-data.sql, optional-append-only-triggers.sql (reference)
├── docs/       database-design.md, ER-diagram.md, project-documentation.md, workflow-documentation.md
├── uploads/    evidence storage (configurable with UPLOAD_DIR, not publicly served)
└── src/
    ├── main/java/com/cybershield/
    │   ├── CyberShieldApplication.java
    │   ├── config/      DemoDataInitializer, DemoDataService, SchedulingConfig
    │   ├── security/    SecurityConfig, UserDetailsService, login/logout audit handlers, Roles
    │   ├── controller/  MVC controllers (+ controller/api: small JSON endpoints)
    │   ├── service/     business logic and transactions
    │   ├── repository/  Spring Data JPA repositories (JPQL with joins / GROUP BY)
    │   ├── entity/      29 JPA entities (one per table)
    │   ├── dto/         forms (validated) and read-only view records
    │   ├── enums/       status / type enums stored as strings
    │   ├── exception/   business exceptions + @ControllerAdvice handlers
    │   ├── mapper/      entity → view mapping
    │   ├── report/      CSV and PDF writers
    │   ├── scheduler/   deadline reminder job
    │   ├── storage/     secure local file storage
    │   ├── util/, validation/
    ├── main/resources/
    │   ├── application.properties, application-dev.properties, application-test.properties
    │   ├── db/migration/   Flyway V1–V10
    │   ├── demo/demo-data.sql   idempotent sample records (dev profile)
    │   ├── templates/      Thymeleaf pages + fragments/layout.html
    │   └── static/         css, js, images, vendor (Bootstrap, Bootstrap Icons, Chart.js)
    └── test/java/com/cybershield/   unit + MySQL integration tests
```

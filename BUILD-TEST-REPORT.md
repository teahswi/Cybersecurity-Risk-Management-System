# CyberShield — Build and Test Report

**Read this first.** It states exactly what was and was not verified while this project was produced.
Nothing listed under "Not executed" should be assumed to work until you run it yourself.

## 1. Build environment and the key limitation

| Item | Value |
|---|---|
| Build workspace OS | Linux (Ubuntu 24.04), cloud sandbox |
| JDK | OpenJDK 21.0.12 |
| Maven | 3.9.11 (local install, used only to test the `mvnw` script) |
| MySQL | **8.0.46** (installed in the sandbox; same major version as required) |
| Network | **Maven Central (`repo.maven.apache.org`) was blocked by the sandbox network policy** |

Because Maven Central was unreachable, **no Java dependency (Spring Boot 4.1.2, Hibernate, Thymeleaf, PDFBox, JUnit …)
could be downloaded**. Therefore:

* `mvnw.cmd clean verify` / `./mvnw clean verify` was **NOT executed**.
* The application was **never started**, and **no JUnit test was run**.

Everything that *could* be verified without those libraries was verified, as listed below.

## 2. What was executed and passed

| # | Check | Command / method | Result |
|---|---|---|---|
| 1 | All 10 Flyway migrations on real MySQL 8.0.46, run as the non-root `cybershield_user` with exactly the privileges from the README | `mysql -u cybershield_user cybershield_db < V1…V10` | **PASS** – 29 tables + 2 views created, 19 controls seeded |
| 2 | Demo data script (`demo/demo-data.sql`) on top of the migrations, with the 6 demo users | run **twice** | **PASS** – no errors, no duplicates (idempotent); all CHECK constraints satisfied |
| 3 | Reference scripts on a fresh empty database | `schema.sql` + `sample-data.sql` (twice) | **PASS** – 6 users, 13 assets, 6 tasks, 8 notifications |
| 4 | Optional append-only triggers (as root) | `UPDATE audit_logs …` | **PASS** – rejected with `audit_logs is append-only` |
| 5 | CHECK constraint enforcement | insert risk with score 19 for 4×5; set an unassigned task to COMPLETED | **PASS** – both rejected (`chk_risk_score`, `chk_task_assignee`) |
| 6 | Trigger privilege behaviour with binary logging ON | `CREATE TRIGGER` as `cybershield_user` | Confirmed error 1419 → triggers moved to an optional root script, not Flyway |
| 7 | Entity ↔ schema consistency (stand-in for Hibernate `ddl-auto=validate`) | script comparing all 29 entity mappings with `information_schema.columns` | **PASS** – 29/29 tables, every column mapped, types/lengths/nullability match, 0 problems |
| 8 | Java syntax of all 214 source files (main + test) | `javac` parser API (`JavacTask.parse`) | **PASS** – 0 syntax errors |
| 9 | Java type-checking of every reference **between project classes** (records, DTOs, services, repositories, controllers, tests) | `javac` without the external jars, then filtering out only "missing library" errors | **PASS** – 0 internal errors. The check was validated by injecting 3 deliberate mistakes (wrong record arity, wrong constructor arguments, misspelled setter); all 3 were reported. |
| 10 | Thymeleaf templates: tag balance, expression brace/quote balance | HTML parser script over 47 templates | **PASS** |
| 11 | Every `${obj.property}` used in templates exists on a Java view/form/enum class | script | **PASS** |
| 12 | Every model attribute used by a template is provided by a controller/advice | script | **PASS** |
| 13 | Mermaid ER diagram syntax | `mermaid.parse()` (mermaid 11) | **PASS** |
| 14 | `mvnw` (POSIX) wrapper logic | pre-placed Maven distribution in the wrapper cache, `./mvnw -v` | **PASS** – resolves the cache path and runs Maven 3.9.11 |
| 15 | Frontend libraries | downloaded from npm and copied into `static/vendor` | Bootstrap 5.3.8, Bootstrap Icons 1.13.2, Chart.js 4.6.1 (no CDN needed) |
| 16 | No placeholders / secrets | grep for TODO/FIXME and passwords in properties | none found |

## 3. Not executed (must be verified on your machine)

| Item | Why | How to verify |
|---|---|---|
| Compilation against Spring Boot 4.1.2 / Spring Framework 7 / Spring Security 7 / Hibernate 7 / PDFBox 3.0.7 | dependencies could not be downloaded | `mvnw.cmd clean verify` |
| All 58 JUnit tests (17 unit, 41 integration against `cybershield_test`) | same | `mvnw.cmd clean verify` |
| Hibernate schema validation at start-up | same | start the app; it fails fast if a mapping is wrong |
| Execution of the JPQL queries | same | covered by the integration tests |
| Thymeleaf rendering of all 47 templates | same | covered by `PageRenderingTest` (renders every page for every role) |
| PDF generation | same | covered by `ReportWriterTest` and `ReportDashboardSearchTest` |
| UI in a browser (layout, dark mode, charts, notification polling) | app not started | follow README §18 |
| `mvnw.cmd` on Windows | no Windows/PowerShell in the sandbox | `mvnw.cmd -v` |
| Existence of the Spring Boot 4.1.2 artifacts as named in the specification (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, `spring-boot-starter-security-test`) | Maven Central unreachable | first Maven import in IntelliJ |

## 4. Test suite written

| Test class | Type | Tests | Covers (spec §32 item numbers) |
|---|---|---:|---|
| `TaskStatusTest` | unit | 3 | 15, 16 |
| `RiskScoreTest` | unit | 3 | 26 |
| `UtilitiesTest` | unit | 3 | IP validation, version comparison |
| `FileStorageServiceTest` | unit | 6 | upload validation, size limit, path traversal |
| `ReportWriterTest` | unit | 2 | CSV escaping/injection, PDF pages |
| `ApplicationAndDatabaseTest` | integration (MySQL) | 4 | 1, 2, 3, 5 |
| `AuthenticationAndAuthorizationTest` | integration (MySQL + MockMvc) | 9 | 4, 6, 7, 25 |
| `AssetAndVulnerabilityTest` | integration | 6 | 8, 9, 10, 11 |
| `RemediationWorkflowTest` | integration | 5 | 12, 13, 14, 16, 17, 18, 19, 20, 28 |
| `NotificationIncidentAuditRiskTest` | integration | 6 | 20, 21, 22, 23, 24, 25, 26, 27 |
| `ReportDashboardSearchTest` | integration | 5 | dashboard, search filtering, reports, health |
| `PageRenderingTest` | integration (MockMvc) | 6 | every page rendered per role, REST JSON, CSRF, SRV-104 over HTTP |
| **Total** | | **58** | all 28 items |

Integration tests use **only** `cybershield_test` (a guard aborts otherwise), load the demo data once, and roll back
every test's transaction.

## 5. How to run the verification

```cmd
:: once, in MySQL Workbench: create cybershield_db, cybershield_test and cybershield_user (README §9-10)
cd CyberShield
set DB_PASSWORD=your_password
mvnw.cmd clean verify
```

Expected: `Tests run: 58, Failures: 0, Errors: 0` and `BUILD SUCCESS`. Then start the application
(`mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`) and follow the SRV-104 walkthrough (README §18).

If anything fails, the error message plus the failing test name is enough to fix it — please paste them back.

## 6. Known limitations and points to watch

1. **Unverified against the real libraries** (see §3). The code relies on standard, long-stable APIs (Spring MVC,
   Spring Data JPA, Spring Security `SecurityFilterChain` lambda DSL, `@PreAuthorize`, PDFBox 3 `Loader` /
   `Standard14Fonts`), and avoids areas known to have changed in Spring Boot 4 (Boot's MockMvc/JPA test
   auto-configuration packages, Jackson-specific code, Thymeleaf Security extras, `Specification` API).
   MockMvc is built manually with `MockMvcBuilders` for that reason.
2. **Enum columns and Hibernate validation**: enum fields declare `columnDefinition = "VARCHAR(n)"` so Hibernate's
   schema validator compares them with the `VARCHAR` columns created by Flyway (Hibernate's MySQL dialect would
   otherwise expect native `ENUM` columns).
3. **Triggers are optional** – MySQL 8 refuses `CREATE TRIGGER` for a non-SUPER user while binary logging is on, so the
   append-only rule is enforced by the application (no update/delete repository methods, `@Immutable` entities) and,
   optionally, by `database/optional-append-only-triggers.sql` run as root.
4. **Alerts are stored data** – there is no live SIEM/EDR integration; the README and UI say so.
5. **Demo data** – vulnerabilities are clearly marked `[DEMO]` and have no CVE identifiers; all names are fictional.
6. Deactivating a user prevents new logins; an already open session of that user lasts until it expires (30 min)
   or they log out.
7. Generated codes (e.g. `REM-0007`) come from the row id, so gaps appear if a transaction is rolled back. This is
   expected with auto-increment keys.
8. `mvnw.cmd` was written to match the official Maven Wrapper 3.3.x "only-script" launcher but could not be run on
   Windows here. If it ever fails, IntelliJ's bundled Maven works as a fallback (*Settings → Maven → Maven home path*).

## 7. Packaging

`CyberShield.zip` extracts to a single `CyberShield/` folder containing source, templates, static assets (with vendor
libraries), migrations, reference SQL, properties, `pom.xml`, the Maven Wrapper, tests, README, documentation and this
report. Excluded: `target/`, IDE folders, local passwords, uploaded files.

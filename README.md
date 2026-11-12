# CyberShield

CyberShield is a database-backed cybersecurity management platform for managing assets,
vulnerabilities, remediation, incidents, compliance, risk, reporting, and audit history.

## Requirements

- Java 21
- MySQL 8.x
- Internet access for the first Maven dependency download

Maven is provided through the Maven Wrapper. No separate Maven installation is required.

## Technology

- Java 21
- Spring Boot 4.1.2
- Spring MVC, Spring Data JPA, Hibernate, Spring Security
- Thymeleaf, Bootstrap, Chart.js
- MySQL 8.x and Flyway
- PDFBox for PDF reports
- JUnit 5, AssertJ, Spring Boot Test, MockMvc

## Project layout

```text
src/main/java/com/cybershield/   Application source
src/main/resources/              Configuration, migrations, templates, static assets
src/test/                         Unit and MySQL integration tests
database/                         Reference schema, sample data, optional triggers
docs/                             Design and workflow documentation
uploads/                          Local evidence-file storage
pom.xml                           Maven build configuration
```

## Database setup

Start MySQL, then create the databases and application user:

```sql
CREATE DATABASE cybershield_db;
CREATE DATABASE cybershield_test;

CREATE USER 'cybershield_user'@'localhost'
IDENTIFIED BY 'ChangeThisLocalPassword!';

GRANT ALL PRIVILEGES ON cybershield_db.* TO 'cybershield_user'@'localhost';
GRANT ALL PRIVILEGES ON cybershield_test.* TO 'cybershield_user'@'localhost';
FLUSH PRIVILEGES;
```

Use a private password for real environments. The example password above is for local development only.

Flyway creates and updates the schema automatically when the application or tests start.
Do not run `database/schema.sql` or `database/sample-data.sql` on a database already managed by Flyway.

## Configuration

The application reads credentials from environment variables:

| Variable | Default / purpose |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cybershield_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `cybershield_user` |
| `DB_PASSWORD` | Required password for the database user |
| `UPLOAD_DIR` | `./uploads` |
| `TEST_DB_URL` | Test database URL; defaults to `cybershield_test` |
| `TEST_DB_USERNAME` | Test username; defaults to `DB_USERNAME` |
| `TEST_DB_PASSWORD` | Test password; defaults to `DB_PASSWORD` |

Never commit passwords or other credentials to the repository.

## Run the application

### Windows Command Prompt

```cmd
set DB_PASSWORD=your_password
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### PowerShell

```powershell
$env:DB_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Open [http://localhost:8080](http://localhost:8080). If port 8080 is unavailable:

```powershell
.\mvnw.cmd spring-boot:run `
  "-Dspring-boot.run.profiles=dev" `
  "-Dspring-boot.run.arguments=--server.port=8081"
```

The `dev` profile enables idempotent demo data. It is the default profile, but should not be used
for production deployments.

## Demo accounts

Demo accounts are created only in the `dev` profile:

| Role | Username | Password |
|---|---|---|
| Security Analyst | `analyst` | `Analyst@123` |
| IT Infrastructure Engineer | `engineer` | `Engineer@123` |
| Compliance Auditor | `auditor` | `Auditor@123` |
| Security Manager | `manager` | `Manager@123` |
| Security Analyst | `analyst2` | `Demo@12345` |
| IT Infrastructure Engineer | `engineer2` | `Demo@12345` |

These credentials are for local demonstrations only.

## Test and build

Integration tests use `cybershield_test` and refuse to run against another database.

```cmd
set DB_PASSWORD=your_password
mvnw.cmd clean verify
```

This command compiles the application, runs unit and integration tests, and produces the packaged JAR.

## Database migrations

Flyway migrations are located in `src/main/resources/db/migration`:

| Migration | Scope |
|---|---|
| V1 | Users, roles, departments, offices, frameworks |
| V2 | Assets, software, installations |
| V3 | Vulnerabilities and affected assets |
| V4 | Remediation tasks, evidence, and history |
| V5 | Alerts and incidents |
| V6 | Audits, findings, controls, and corrective actions |
| V7 | Risk assessments and treatments |
| V8 | Notifications and audit logs |
| V9 | Indexes and reporting views |
| V10 | Reference data |

Never edit a migration that has already been applied. Add a new versioned migration instead.
Hibernate validates the schema with `ddl-auto=validate`; it does not modify database structure.

Optional append-only database triggers are provided in
[`database/optional-append-only-triggers.sql`](database/optional-append-only-triggers.sql).
Run them as a privileged MySQL user if database-level enforcement is required.

## Core capabilities

- Role-based authentication and authorization with BCrypt
- Asset and software inventory
- Vulnerability tracking and asset-level risk assessment
- Remediation workflow with evidence upload and review
- Security alerts and incident investigations
- Compliance audits, findings, corrective actions, and evidence
- Risk register with server-side likelihood × impact scoring
- Notifications, scheduled deadline reminders, and append-only audit logs
- Role-filtered search and dashboard data
- CSV and PDF reports
- Light and dark themes

Backend authorization, CSRF protection, ownership rules, and workflow transitions are enforced
server-side.

## Documentation

- [`docs/project-documentation.md`](docs/project-documentation.md) — system overview
- [`docs/database-design.md`](docs/database-design.md) — schema and relationships
- [`docs/workflow-documentation.md`](docs/workflow-documentation.md) — business workflows
- [`docs/ER-diagram.md`](docs/ER-diagram.md) — entity-relationship diagram
- [`BUILD-TEST-REPORT.md`](BUILD-TEST-REPORT.md) — verification history and known limitations

## Troubleshooting

| Problem | Resolution |
|---|---|
| MySQL connection refused | Start MySQL and verify that port 3306 is available. |
| Access denied for `cybershield_user` | Check `DB_USERNAME`, `DB_PASSWORD`, and the user's grants. |
| Unknown database | Create `cybershield_db` and `cybershield_test`. |
| Public key retrieval error | Ensure the JDBC URL contains `allowPublicKeyRetrieval=true`. |
| Flyway validation failure | Restore the applied migration and add a new migration for changes. |
| Port 8080 unavailable | Start with `--server.port=8081` or stop the conflicting process. |
| Demo accounts missing | Start with the `dev` profile. |
| Tests target the wrong database | Check `TEST_DB_URL`; it must point to `cybershield_test`. |
| `JAVA_HOME` or Java version error | Configure JDK 21 as `JAVA_HOME` and the IDE project SDK. |


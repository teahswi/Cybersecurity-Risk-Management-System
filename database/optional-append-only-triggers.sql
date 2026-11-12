-- =====================================================================
-- OPTIONAL: database-level enforcement of the append-only tables.
--
-- The application already treats audit_logs, remediation_history and the review tables as append-only
-- (their repositories have no update/delete methods and the entities are @Immutable).
-- These triggers add a second, database-level guarantee for the DBMS demonstration.
--
-- They are NOT part of the Flyway migrations because MySQL 8 has binary logging enabled by default,
-- and creating a trigger then requires the SUPER privilege (error 1419) which the normal
-- cybershield_user does not have. Run this file once as root in MySQL Workbench:
--     USE cybershield_db;  SOURCE optional-append-only-triggers.sql;
-- To remove them:  DROP TRIGGER trg_audit_logs_no_update; ... (see the names below)
-- =====================================================================

CREATE TRIGGER trg_audit_logs_no_update BEFORE UPDATE ON audit_logs FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';

CREATE TRIGGER trg_audit_logs_no_delete BEFORE DELETE ON audit_logs FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only';

CREATE TRIGGER trg_remediation_history_no_update BEFORE UPDATE ON remediation_history FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'remediation_history is append-only';

CREATE TRIGGER trg_remediation_history_no_delete BEFORE DELETE ON remediation_history FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'remediation_history is append-only';

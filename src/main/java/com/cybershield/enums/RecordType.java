package com.cybershield.enums;

/**
 * Type of record referenced by notifications and audit logs.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum RecordType implements LabeledEnum {
    ASSET, SOFTWARE, VULNERABILITY, ASSET_VULNERABILITY, REMEDIATION_TASK, REMEDIATION_EVIDENCE, ALERT, INCIDENT, AUDIT_PLAN, AUDIT_FINDING, CORRECTIVE_ACTION, AUDIT_EVIDENCE, COMPLIANCE_CONTROL, RISK, USER, DEPARTMENT, OFFICE_LOCATION, REPORT
}

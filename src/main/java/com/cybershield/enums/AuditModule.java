package com.cybershield.enums;

/**
 * Application module recorded in the audit log.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AuditModule implements LabeledEnum {
    AUTH, ASSETS, SOFTWARE, VULNERABILITIES, REMEDIATION, ALERTS, INCIDENTS, AUDITS, RISKS, USERS, PROFILE, SETTINGS, REPORTS, NOTIFICATIONS, SYSTEM
}

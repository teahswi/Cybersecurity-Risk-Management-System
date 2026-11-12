package com.cybershield.enums;

/**
 * Severity used by vulnerabilities, alerts, incidents and audit findings.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum Severity implements LabeledEnum {
    LOW, MEDIUM, HIGH, CRITICAL
}

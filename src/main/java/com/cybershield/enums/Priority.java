package com.cybershield.enums;

/**
 * Priority of a remediation task.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum Priority implements LabeledEnum {
    LOW, MEDIUM, HIGH, CRITICAL
}

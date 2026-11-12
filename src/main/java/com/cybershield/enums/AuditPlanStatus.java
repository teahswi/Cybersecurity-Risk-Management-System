package com.cybershield.enums;

/**
 * Status of an audit plan.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AuditPlanStatus implements LabeledEnum {
    PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
}

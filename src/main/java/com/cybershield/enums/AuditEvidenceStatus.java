package com.cybershield.enums;

/**
 * Status of an audit evidence file.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AuditEvidenceStatus implements LabeledEnum {
    SUBMITTED, VERIFIED, REJECTED
}

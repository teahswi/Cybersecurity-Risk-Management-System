package com.cybershield.enums;

/**
 * Status of a remediation evidence file. DRAFT = uploaded but not yet submitted.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum EvidenceStatus implements LabeledEnum {
    DRAFT, SUBMITTED, APPROVED, REJECTED
}

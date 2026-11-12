package com.cybershield.enums;

/**
 * Decision on audit evidence.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AuditReviewDecision implements LabeledEnum {
    VERIFIED, REJECTED
}

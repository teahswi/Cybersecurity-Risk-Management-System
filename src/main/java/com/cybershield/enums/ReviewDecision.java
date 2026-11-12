package com.cybershield.enums;

/**
 * Decision on submitted remediation evidence.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum ReviewDecision implements LabeledEnum {
    APPROVED, REJECTED
}

package com.cybershield.enums;

/**
 * Status of a corrective action.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum CorrectiveActionStatus implements LabeledEnum {
    OPEN, IN_PROGRESS, PENDING_REVIEW, VERIFIED
}

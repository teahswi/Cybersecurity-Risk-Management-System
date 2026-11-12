package com.cybershield.enums;

/**
 * Status of an audit finding.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum FindingStatus implements LabeledEnum {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}

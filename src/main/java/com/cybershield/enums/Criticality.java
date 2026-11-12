package com.cybershield.enums;

/**
 * Business criticality of an asset.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum Criticality implements LabeledEnum {
    LOW, MEDIUM, HIGH, CRITICAL
}

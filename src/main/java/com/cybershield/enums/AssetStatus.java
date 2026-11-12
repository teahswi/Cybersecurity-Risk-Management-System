package com.cybershield.enums;

/**
 * Lifecycle status of an asset. Assets are archived, never physically deleted.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum AssetStatus implements LabeledEnum {
    ACTIVE, MAINTENANCE, INACTIVE, ARCHIVED
}

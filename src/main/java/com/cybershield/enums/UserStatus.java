package com.cybershield.enums;

/**
 * Account status. INACTIVE accounts cannot log in.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum UserStatus implements LabeledEnum {
    ACTIVE, INACTIVE
}

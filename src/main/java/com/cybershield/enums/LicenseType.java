package com.cybershield.enums;

/**
 * Licence type of an installed software product.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum LicenseType implements LabeledEnum {
    OPEN_SOURCE, COMMERCIAL, SUBSCRIPTION, FREEWARE, ENTERPRISE
}

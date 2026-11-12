package com.cybershield.enums;

/**
 * Deployment environment of an asset.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum Environment implements LabeledEnum {
    PRODUCTION, STAGING, DEVELOPMENT, TEST, DISASTER_RECOVERY
}

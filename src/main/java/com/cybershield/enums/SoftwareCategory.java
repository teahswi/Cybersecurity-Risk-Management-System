package com.cybershield.enums;

/**
 * Software catalogue category.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum SoftwareCategory implements LabeledEnum {
    OPERATING_SYSTEM, DATABASE, WEB_SERVER, APPLICATION_SERVER, SECURITY, PRODUCTIVITY, DEVELOPMENT, NETWORKING, BROWSER, OTHER
}

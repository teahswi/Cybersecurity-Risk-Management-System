package com.cybershield.enums;

/**
 * Application roles. Spring Security authorities are "ROLE_" + name().
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum RoleCode implements LabeledEnum {
    SECURITY_ANALYST, IT_INFRASTRUCTURE_ENGINEER, COMPLIANCE_AUDITOR, SECURITY_MANAGER
}

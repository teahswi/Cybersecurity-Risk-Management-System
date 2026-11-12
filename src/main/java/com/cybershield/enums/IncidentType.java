package com.cybershield.enums;

/**
 * Category of security incident.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum IncidentType implements LabeledEnum {
    MALWARE, PHISHING, UNAUTHORIZED_ACCESS, DATA_BREACH, DENIAL_OF_SERVICE, INSIDER_THREAT, POLICY_VIOLATION, RECONNAISSANCE, OTHER
}

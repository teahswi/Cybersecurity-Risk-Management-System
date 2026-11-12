package com.cybershield.enums;

/**
 * Status of a risk assessment.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum RiskStatus implements LabeledEnum {
    OPEN, TREATMENT_PLANNED, IN_TREATMENT, MONITORING, CLOSED
}

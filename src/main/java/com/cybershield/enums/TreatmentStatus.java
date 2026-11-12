package com.cybershield.enums;

/**
 * Status of a risk treatment action.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum TreatmentStatus implements LabeledEnum {
    PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
}

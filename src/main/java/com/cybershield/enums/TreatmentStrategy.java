package com.cybershield.enums;

/**
 * Risk treatment strategy.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum TreatmentStrategy implements LabeledEnum {
    MITIGATE, AVOID, TRANSFER, ACCEPT
}

package com.cybershield.enums;

/**
 * Risk level derived from Risk Score = Likelihood x Impact (each 1-5).
 * 1-4 Low, 5-9 Medium, 10-16 High, 17-25 Critical.
 * This is the project's documented methodology, not an official certification scheme.
 */
public enum RiskLevel implements LabeledEnum {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel fromScore(int score) {
        if (score < 1 || score > 25) {
            throw new IllegalArgumentException("Risk score must be between 1 and 25");
        }
        if (score <= 4) {
            return LOW;
        }
        if (score <= 9) {
            return MEDIUM;
        }
        if (score <= 16) {
            return HIGH;
        }
        return CRITICAL;
    }
}

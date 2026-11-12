package com.cybershield.enums;

import java.util.EnumSet;
import java.util.Set;

/** Security alert lifecycle with validated transitions. */
public enum AlertStatus implements LabeledEnum {
    NEW, INVESTIGATING, ESCALATED, RESOLVED, CLOSED;

    public Set<AlertStatus> allowedNextStatuses() {
        return switch (this) {
            case NEW -> EnumSet.of(INVESTIGATING, ESCALATED, CLOSED);
            case INVESTIGATING -> EnumSet.of(ESCALATED, RESOLVED);
            case ESCALATED -> EnumSet.of(INVESTIGATING, RESOLVED);
            case RESOLVED -> EnumSet.of(CLOSED, INVESTIGATING);
            case CLOSED -> EnumSet.noneOf(AlertStatus.class);
        };
    }

    public boolean canTransitionTo(AlertStatus next) {
        return next != null && allowedNextStatuses().contains(next);
    }

    public boolean isActive() {
        return this == NEW || this == INVESTIGATING || this == ESCALATED;
    }
}

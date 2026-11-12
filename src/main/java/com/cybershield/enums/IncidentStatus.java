package com.cybershield.enums;

import java.util.EnumSet;
import java.util.Set;

/** Incident investigation lifecycle with validated transitions. */
public enum IncidentStatus implements LabeledEnum {
    OPEN, INVESTIGATING, CONTAINED, RESOLVED, CLOSED;

    public Set<IncidentStatus> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> EnumSet.of(INVESTIGATING);
            case INVESTIGATING -> EnumSet.of(CONTAINED, RESOLVED);
            case CONTAINED -> EnumSet.of(INVESTIGATING, RESOLVED);
            case RESOLVED -> EnumSet.of(CLOSED, INVESTIGATING);
            case CLOSED -> EnumSet.noneOf(IncidentStatus.class);
        };
    }

    public boolean canTransitionTo(IncidentStatus next) {
        return next != null && allowedNextStatuses().contains(next);
    }

    public boolean isOpen() {
        return this != RESOLVED && this != CLOSED;
    }
}

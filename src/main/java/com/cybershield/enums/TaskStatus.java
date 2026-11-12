package com.cybershield.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Remediation task state machine.
 *
 * <pre>
 * OPEN -> ASSIGNED -> IN_PROGRESS -> PENDING_VERIFICATION -> COMPLETED
 *                          ^                  |                   |
 *                          |   (rejected)     |                   v
 *                          +------------------+               REOPENED
 *                          ^                                      |
 *                          +--------------------------------------+
 * </pre>
 * The service layer calls {@link #canTransitionTo(TaskStatus)} before every status change,
 * so arbitrary jumps such as OPEN -> COMPLETED are impossible.
 */
public enum TaskStatus implements LabeledEnum {
    OPEN, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION, COMPLETED, REOPENED;

    public Set<TaskStatus> allowedNextStatuses() {
        return switch (this) {
            case OPEN -> EnumSet.of(ASSIGNED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(PENDING_VERIFICATION);
            case PENDING_VERIFICATION -> EnumSet.of(COMPLETED, IN_PROGRESS);
            case COMPLETED -> EnumSet.of(REOPENED);
            case REOPENED -> EnumSet.of(IN_PROGRESS);
        };
    }

    public boolean canTransitionTo(TaskStatus next) {
        return next != null && allowedNextStatuses().contains(next);
    }

    /** Tasks in these states are still "pending" work. */
    public boolean isActive() {
        return this != COMPLETED;
    }
}

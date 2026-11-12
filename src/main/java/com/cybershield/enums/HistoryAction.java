package com.cybershield.enums;

/**
 * Type of an entry in the append-only remediation_history table.
 * Stored in MySQL as a string (@Enumerated(EnumType.STRING)), never as an ordinal.
 */
public enum HistoryAction implements LabeledEnum {
    CREATED, ASSIGNED, REASSIGNED, STATUS_CHANGED, NOTE_ADDED, EVIDENCE_UPLOADED, EVIDENCE_SUBMITTED, EVIDENCE_APPROVED, EVIDENCE_REJECTED, REOPENED, UPDATED
}

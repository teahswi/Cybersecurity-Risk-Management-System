package com.cybershield.dto;

import com.cybershield.enums.AlertSource;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.IncidentType;
import com.cybershield.enums.Severity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Views for alerts and incidents. */
public final class IncidentViews {

    private IncidentViews() {
    }

    public record AlertRow(Long id, String alertCode, String title, AlertSource source, Severity severity,
                           String assetCode, LocalDateTime detectedAt, AlertStatus status, String analystName) {
    }

    public record AlertDetail(Long id, String alertCode, String title, String description, AlertSource source,
                              Severity severity, Long assetId, String assetCode, String assetName,
                              LocalDateTime detectedAt, AlertStatus status, Long analystId, String analystName,
                              String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt,
                              Set<AlertStatus> nextStatuses, List<IncidentRow> incidents, List<AuditLogView> history) {
    }

    public record IncidentRow(Long id, String incidentCode, String title, IncidentType incidentType, Severity severity,
                              IncidentStatus status, String analystName, LocalDateTime detectedAt) {
    }

    public record LinkedAlert(Long id, String alertCode, String title, Severity severity, AlertStatus status,
                              LocalDateTime detectedAt, String assetCode) {
    }

    public record AffectedAsset(Long id, String assetCode, String assetName, String departmentName,
                                AssetStatus assetStatus, String impact) {
    }

    /** An entry on the incident timeline: either an investigation note or an audit log event. */
    public record TimelineEntry(LocalDateTime at, String kind, String author, String text) {
    }

    public record IncidentDetail(Long id, String incidentCode, String title, String description, IncidentType incidentType,
                                 Severity severity, IncidentStatus status, Long analystId, String analystName,
                                 LocalDateTime detectedAt, String resolutionDetails, LocalDateTime resolvedAt,
                                 String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt,
                                 Set<IncidentStatus> nextStatuses, List<LinkedAlert> alerts,
                                 List<AffectedAsset> assets, List<TimelineEntry> timeline, boolean canEdit) {
    }
}

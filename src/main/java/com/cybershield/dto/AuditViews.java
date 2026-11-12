package com.cybershield.dto;

import com.cybershield.enums.AuditEvidenceStatus;
import com.cybershield.enums.AuditPlanStatus;
import com.cybershield.enums.AuditReviewDecision;
import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.Severity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Views for audit plans, frameworks, controls, findings and corrective actions. */
public final class AuditViews {

    private AuditViews() {
    }

    public record PlanRow(Long id, String planCode, String name, String frameworkName, String auditorName,
                          LocalDate startDate, LocalDate targetCompletionDate, AuditPlanStatus status) {
    }

    public record FindingRow(Long id, String findingCode, String title, String planCode, String controlCode,
                             Severity severity, FindingStatus status, LocalDate dueDate) {
    }

    public record PlanDetail(Long id, String planCode, String name, Long frameworkId, String frameworkName,
                             String description, String auditorName, Long auditorId, LocalDate startDate,
                             LocalDate targetCompletionDate, AuditPlanStatus status, String createdByName,
                             LocalDateTime createdAt, Set<AuditPlanStatus> nextStatuses, List<FindingRow> findings,
                             boolean canEdit, List<AuditLogView> history) {
    }

    public record Framework(Long id, String code, String name, String versionLabel, String description, long controls) {
    }

    public record Control(Long id, String frameworkName, String controlCode, String title, String description,
                          String category) {
    }

    public record ActionRow(Long id, String findingCode, String findingTitle, String assignedToName, String description,
                            LocalDate deadline, CorrectiveActionStatus status, boolean overdue) {
    }

    public record FindingDetail(Long id, String findingCode, String title, String description, Severity severity,
                                FindingStatus status, LocalDate dueDate, Long planId, String planCode, String planName,
                                String frameworkName, String controlCode, String controlTitle, String createdByName,
                                LocalDateTime createdAt, List<ActionRow> actions, boolean canEdit,
                                List<AuditLogView> history) {
    }

    public record EvidenceLine(Long id, String originalFilename, long fileSize, String description,
                               AuditEvidenceStatus status, String submittedBy, Long submittedById,
                               LocalDateTime submittedAt) {
    }

    public record ReviewLine(String evidenceFilename, String reviewer, AuditReviewDecision decision, String notes,
                             LocalDateTime reviewedAt) {
    }

    public record ActionDetail(Long id, Long findingId, String findingCode, String findingTitle, String planCode,
                               String auditorName, String assignedToName, Long assignedToId, String description,
                               LocalDate deadline, CorrectiveActionStatus status, boolean overdue, String createdByName,
                               LocalDateTime createdAt, List<EvidenceLine> evidence, List<ReviewLine> reviews,
                               boolean canStart, boolean canUpload, boolean canReview) {
    }
}

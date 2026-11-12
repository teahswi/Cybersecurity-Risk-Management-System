package com.cybershield.dto;

import com.cybershield.enums.Criticality;
import com.cybershield.enums.EvidenceStatus;
import com.cybershield.enums.Environment;
import com.cybershield.enums.HistoryAction;
import com.cybershield.enums.Priority;
import com.cybershield.enums.ReviewDecision;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RemediationViews {

    private RemediationViews() {
    }

    public record Row(Long id, String taskCode, String title, Long assetId, String assetCode, String assetName,
                      String vulnCode, Severity severity, Priority priority, TaskStatus status, String engineerName,
                      LocalDate dueDate, boolean overdue) {
    }

    public record Detail(Long id, String taskCode, String title, String description, Priority priority,
                         TaskStatus status, LocalDate dueDate, boolean overdue, LocalDateTime completedAt,
                         LocalDateTime createdAt, LocalDateTime updatedAt, String createdByName, Long engineerId,
                         String engineerName, Long assetVulnerabilityId, VulnerabilityStatus vulnerabilityStatus,
                         Long assetId, String assetCode, String assetName, String hostname, String ipAddress,
                         String operatingSystem, String departmentName, String locationName, Criticality criticality,
                         Environment environment, Long vulnerabilityId, String vulnCode, String vulnTitle,
                         Severity severity, BigDecimal cvssScore, String recommendedRemediation) {

        /** Position in the workflow stepper: Open(0) Assigned(1) In Progress(2) Pending Verification(3) Completed(4). */
        public int getStep() {
            return switch (status) {
                case OPEN -> 0;
                case ASSIGNED -> 1;
                case IN_PROGRESS, REOPENED -> 2;
                case PENDING_VERIFICATION -> 3;
                case COMPLETED -> 4;
            };
        }
    }

    public record HistoryLine(LocalDateTime createdAt, HistoryAction actionType, TaskStatus previousStatus,
                              TaskStatus newStatus, String previousAssignee, String newAssignee, String performedBy,
                              String description) {
    }

    public record EvidenceLine(Long id, String originalFilename, String mimeType, long fileSize, String description,
                               EvidenceStatus status, String submittedBy, LocalDateTime submittedAt) {

        public String getSizeLabel() {
            return fileSize >= 1024 * 1024 ? String.format("%.1f MB", fileSize / (1024.0 * 1024))
                    : String.format("%.1f KB", fileSize / 1024.0);
        }
    }

    public record ReviewLine(String evidenceFilename, String reviewer, ReviewDecision decision, String comments,
                             LocalDateTime reviewedAt) {
    }

    /** What the current user may do on this task. The service re-checks every action anyway. */
    public record Permissions(boolean canAssign, boolean canStart, boolean canAddNote, boolean canUpload,
                              boolean canSubmit, boolean canReview, boolean canReopen, boolean canDownload) {
    }

    public record TaskPage(Detail task, List<HistoryLine> history, List<EvidenceLine> evidence,
                           List<ReviewLine> reviews, Permissions permissions, boolean hasDraftEvidence) {
    }

    public record Progress(long total, long open, long assigned, long inProgress, long pendingVerification,
                           long completed, long reopened, long overdue, int completionPercent) {
    }
}

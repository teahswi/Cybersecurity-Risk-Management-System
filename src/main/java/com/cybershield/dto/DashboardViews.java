package com.cybershield.dto;

import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardViews {

    private DashboardViews() {
    }

    /** A summary card. tone is a CSS modifier (primary, danger, warning, success, info, secondary). */
    public record Card(String label, long value, String icon, String tone, String link, String hint) {
    }

    public record QuickAction(String label, String icon, String link) {
    }

    public record Deadline(Long id, String code, String title, String context, LocalDate dueDate, long daysLeft,
                           String status, String link) {

        public boolean isOverdue() {
            return daysLeft < 0;
        }
    }

    public record CriticalVulnerability(Long instanceId, String vulnCode, String title, String assetCode,
                                        BigDecimal cvssScore, Severity severity, VulnerabilityStatus status,
                                        LocalDate discoveredDate) {
    }

    public record PendingReview(Long id, String code, String title, String engineerName, String assetCode,
                                TaskStatus status) {
    }

    public record RoleCount(String role, long count) {
    }

    public record Dashboard(List<Card> cards, RemediationViews.Progress progress, List<QuickAction> quickActions,
                            List<AuditLogView> activity, List<NotificationView> notifications,
                            List<Deadline> deadlines, List<CriticalVulnerability> criticalVulnerabilities,
                            List<IncidentViews.IncidentRow> recentIncidents, List<PendingReview> pendingReviews,
                            List<RiskViews.Row> topRisks, List<AuditViews.PlanRow> activePlans,
                            List<RoleCount> usersByRole, long totalUsers, boolean databaseUp) {
    }

    public record ActionDeadline(Long id, String findingCode, String description, LocalDate deadline,
                                 CorrectiveActionStatus status) {
    }
}

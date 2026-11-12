package com.cybershield.service;

import com.cybershield.dto.ChartData;
import com.cybershield.dto.DashboardViews;
import com.cybershield.dto.IncidentViews;
import com.cybershield.dto.RemediationViews;
import com.cybershield.dto.RiskViews;
import com.cybershield.dto.AuditViews;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AuditEvidenceStatus;
import com.cybershield.enums.AuditPlanStatus;
import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.LabeledEnum;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.UserStatus;
import com.cybershield.enums.VulnerabilityStatus;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.AuditEvidenceRepository;
import com.cybershield.repository.AuditFindingRepository;
import com.cybershield.repository.AuditPlanRepository;
import com.cybershield.repository.CorrectiveActionRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.repository.VulnerabilityRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.SecurityUser;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role-aware security dashboard. Every number and chart is calculated from MySQL with COUNT / GROUP BY
 * queries each time the page (or the chart API) is requested - nothing is hard-coded.
 */
@Service
public class DashboardService {

    private static final EnumSet<VulnerabilityStatus> OPEN_VULN = EnumSet.of(VulnerabilityStatus.OPEN, VulnerabilityStatus.MITIGATING);

    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final AssetVulnerabilityRepository assetVulnerabilityRepository;
    private final IncidentRepository incidentRepository;
    private final SecurityAlertRepository alertRepository;
    private final RemediationTaskRepository taskRepository;
    private final AuditFindingRepository findingRepository;
    private final AuditPlanRepository planRepository;
    private final CorrectiveActionRepository actionRepository;
    private final AuditEvidenceRepository auditEvidenceRepository;
    private final RiskAssessmentRepository riskRepository;
    private final UserRepository userRepository;
    private final RemediationService remediationService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SystemService systemService;
    private final CurrentUserService currentUserService;

    public DashboardService(AssetRepository assetRepository, VulnerabilityRepository vulnerabilityRepository,
                            AssetVulnerabilityRepository assetVulnerabilityRepository, IncidentRepository incidentRepository,
                            SecurityAlertRepository alertRepository, RemediationTaskRepository taskRepository,
                            AuditFindingRepository findingRepository, AuditPlanRepository planRepository,
                            CorrectiveActionRepository actionRepository, AuditEvidenceRepository auditEvidenceRepository,
                            RiskAssessmentRepository riskRepository, UserRepository userRepository,
                            RemediationService remediationService, AuditLogService auditLogService,
                            NotificationService notificationService, SystemService systemService,
                            CurrentUserService currentUserService) {
        this.assetRepository = assetRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.assetVulnerabilityRepository = assetVulnerabilityRepository;
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.taskRepository = taskRepository;
        this.findingRepository = findingRepository;
        this.planRepository = planRepository;
        this.actionRepository = actionRepository;
        this.auditEvidenceRepository = auditEvidenceRepository;
        this.riskRepository = riskRepository;
        this.userRepository = userRepository;
        this.remediationService = remediationService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.systemService = systemService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardViews.Dashboard build() {
        SecurityUser me = currentUserService.require();
        RoleCode role = me.getRoleCode();
        boolean manager = role == RoleCode.SECURITY_MANAGER;
        boolean analyst = role == RoleCode.SECURITY_ANALYST;
        boolean engineer = role == RoleCode.IT_INFRASTRUCTURE_ENGINEER;
        boolean auditor = role == RoleCode.COMPLIANCE_AUDITOR;
        LocalDate today = LocalDate.now();

        RemediationViews.Progress progress = remediationService.progressFor(engineer ? me.getId() : null);
        List<DashboardViews.Card> cards = new ArrayList<>();
        long totalAssets = assetRepository.countByStatusNot(AssetStatus.ARCHIVED);
        long openVulns = assetVulnerabilityRepository.countByStatusIn(OPEN_VULN);
        long criticalVulns = assetVulnerabilityRepository.countByStatusInAndSeverity(OPEN_VULN, Severity.CRITICAL);
        long openIncidents = incidentRepository.countByStatusIn(EnumSet.of(IncidentStatus.OPEN,
                IncidentStatus.INVESTIGATING, IncidentStatus.CONTAINED));
        long activeAlerts = alertRepository.countByStatusIn(EnumSet.of(AlertStatus.NEW, AlertStatus.INVESTIGATING,
                AlertStatus.ESCALATED));
        long pendingTasks = progress.total() - progress.completed();
        long openFindings = findingRepository.countByStatusIn(EnumSet.of(FindingStatus.OPEN, FindingStatus.IN_PROGRESS));

        if (manager || analyst) {
            cards.add(new DashboardViews.Card("Total assets", totalAssets, "bi-hdd-network", "primary", "/assets",
                    "Active, maintenance and inactive"));
            cards.add(new DashboardViews.Card("Open vulnerabilities", openVulns, "bi-bug", "warning",
                    "/vulnerabilities?status=OPEN", vulnerabilityRepository.count() + " definitions recorded"));
            cards.add(new DashboardViews.Card("Critical vulnerabilities", criticalVulns, "bi-exclamation-octagon", "danger",
                    "/vulnerabilities?severity=CRITICAL", "Open critical findings"));
            cards.add(new DashboardViews.Card("Open incidents", openIncidents, "bi-fire", "danger", "/incidents",
                    "Open, investigating or contained"));
            cards.add(new DashboardViews.Card("Active security alerts", activeAlerts, "bi-bell", "info", "/alerts",
                    "New, investigating or escalated"));
            cards.add(new DashboardViews.Card("Pending remediation tasks", pendingTasks, "bi-tools", "primary",
                    "/remediation", progress.pendingVerification() + " waiting for verification"));
            cards.add(new DashboardViews.Card("Overdue tasks", progress.overdue(), "bi-alarm", "danger",
                    "/remediation?overdue=true", "Past their deadline"));
            cards.add(new DashboardViews.Card("Open audit findings", openFindings, "bi-journal-check", "secondary",
                    manager ? "/audits/findings" : null, "Open or in progress"));
        } else if (engineer) {
            cards.add(new DashboardViews.Card("My active tasks", pendingTasks, "bi-clipboard-check", "primary",
                    "/remediation", "Not yet completed"));
            cards.add(new DashboardViews.Card("Assigned to start", progress.assigned() + progress.reopened(), "bi-play-circle",
                    "info", "/remediation?status=ASSIGNED", "Assigned or reopened"));
            cards.add(new DashboardViews.Card("In progress", progress.inProgress(), "bi-gear", "warning",
                    "/remediation?status=IN_PROGRESS", "Currently being remediated"));
            cards.add(new DashboardViews.Card("Pending verification", progress.pendingVerification(), "bi-hourglass-split",
                    "secondary", "/remediation?status=PENDING_VERIFICATION", "Waiting for a reviewer"));
            cards.add(new DashboardViews.Card("Overdue tasks", progress.overdue(), "bi-alarm", "danger",
                    "/remediation?overdue=true", "Past their deadline"));
            cards.add(new DashboardViews.Card("Completed", progress.completed(), "bi-check2-circle", "success",
                    "/remediation?status=COMPLETED", progress.completionPercent() + "% of my tasks"));
        } else if (auditor) {
            cards.add(new DashboardViews.Card("Active audit plans", planRepository.countByStatusIn(
                    EnumSet.of(AuditPlanStatus.PLANNED, AuditPlanStatus.IN_PROGRESS)), "bi-clipboard-data", "primary",
                    "/audits", "Planned or in progress"));
            cards.add(new DashboardViews.Card("Open findings", openFindings, "bi-journal-x", "warning",
                    "/audits/findings", "Open or in progress"));
            cards.add(new DashboardViews.Card("Evidence awaiting review", auditEvidenceRepository.countByStatus(
                    AuditEvidenceStatus.SUBMITTED), "bi-file-earmark-check", "danger",
                    "/audits/actions?status=PENDING_REVIEW", "Submitted audit evidence"));
            cards.add(new DashboardViews.Card("Open corrective actions", actionRepository.countByStatusIn(EnumSet.of(
                    CorrectiveActionStatus.OPEN, CorrectiveActionStatus.IN_PROGRESS, CorrectiveActionStatus.PENDING_REVIEW)),
                    "bi-list-check", "info", "/audits/actions", "Not yet verified"));
            cards.add(new DashboardViews.Card("Assets in scope", totalAssets, "bi-hdd-network", "secondary", "/assets",
                    "Non-archived assets"));
        }

        List<DashboardViews.Deadline> deadlines = new ArrayList<>();
        if (!auditor) {
            taskRepository.findDueBetween(today.minusDays(30), today.plusDays(7), engineer ? me.getId() : null,
                            PageRequest.of(0, 6)).forEach(t -> deadlines.add(new DashboardViews.Deadline(t.getId(),
                    t.getTaskCode(), t.getTitle(), t.getAssetVulnerability().getAsset().getAssetCode() + " - "
                    + (t.getAssignedEngineer() == null ? "unassigned" : t.getAssignedEngineer().getFullName()),
                    t.getDueDate(), ChronoUnit.DAYS.between(today, t.getDueDate()), t.getStatus().getLabel(),
                    "/remediation/" + t.getId())));
        } else {
            actionRepository.findOpenOrderByDeadline(PageRequest.of(0, 6)).forEach(c -> deadlines.add(
                    new DashboardViews.Deadline(c.getId(), c.getFinding().getFindingCode(), c.getDescription(),
                            "Corrective action", c.getDeadline(), ChronoUnit.DAYS.between(today, c.getDeadline()),
                            c.getStatus().getLabel(), "/audits/actions/" + c.getId())));
        }

        List<DashboardViews.CriticalVulnerability> critical = (manager || analyst)
                ? assetVulnerabilityRepository.findOpenBySeverity(OPEN_VULN, Severity.CRITICAL, PageRequest.of(0, 5)).stream()
                .map(av -> new DashboardViews.CriticalVulnerability(av.getId(), av.getVulnerability().getVulnCode(),
                        av.getVulnerability().getTitle(), av.getAsset().getAssetCode(), av.getVulnerability().getCvssScore(),
                        av.getVulnerability().getSeverity(), av.getStatus(), av.getDiscoveredDate())).toList()
                : List.of();
        List<IncidentViews.IncidentRow> incidents = (manager || analyst)
                ? incidentRepository.findRecent(PageRequest.of(0, 5)).stream().map(i -> new IncidentViews.IncidentRow(
                        i.getId(), i.getIncidentCode(), i.getTitle(), i.getIncidentType(), i.getSeverity(), i.getStatus(),
                        ViewMapper.nameOf(i.getAssignedAnalyst()), i.getDetectedAt())).toList()
                : List.of();
        List<DashboardViews.PendingReview> reviews = (manager || analyst)
                ? taskRepository.search(null, TaskStatus.PENDING_VERIFICATION, null, null, null,
                        PageRequest.of(0, 5, Sort.by("updatedAt"))).stream()
                .map(t -> new DashboardViews.PendingReview(t.getId(), t.getTaskCode(), t.getTitle(),
                        ViewMapper.nameOf(t.getAssignedEngineer()), t.getAssetVulnerability().getAsset().getAssetCode(),
                        t.getStatus())).toList()
                : List.of();
        List<RiskViews.Row> topRisks = engineer ? List.of()
                : riskRepository.findTopOpen(PageRequest.of(0, 5)).stream().map(r -> new RiskViews.Row(r.getId(),
                        r.getRiskCode(), r.getTitle(), r.getAsset().getAssetCode(), r.getLikelihood(), r.getImpact(),
                        r.getRiskScore(), r.getRiskLevel(), r.getTreatmentStrategy(), null, r.getStatus())).toList();
        List<AuditViews.PlanRow> plans = (auditor || manager)
                ? planRepository.findByStatusIn(EnumSet.of(AuditPlanStatus.PLANNED, AuditPlanStatus.IN_PROGRESS)).stream()
                .limit(5).map(p -> new AuditViews.PlanRow(p.getId(), p.getPlanCode(), p.getName(),
                        p.getFramework().getName(), null, p.getStartDate(), p.getTargetCompletionDate(), p.getStatus()))
                .toList()
                : List.of();
        List<DashboardViews.RoleCount> byRole = new ArrayList<>();
        if (manager) {
            for (Object[] row : userRepository.countActiveByRole()) {
                byRole.add(new DashboardViews.RoleCount(((RoleCode) row[0]).getLabel(), (Long) row[1]));
            }
        }
        return new DashboardViews.Dashboard(cards, progress, quickActions(role),
                manager ? auditLogService.recent(8) : auditLogService.recentByUser(me.getId(), 8),
                notificationService.recentForCurrentUser(5), deadlines, critical, incidents, reviews, topRisks, plans,
                byRole, manager ? userRepository.countByStatus(UserStatus.ACTIVE) : 0,
                manager && systemService.isDatabaseUp());
    }

    /** Chart data for Chart.js (returned as JSON by DashboardApiController). Only role-appropriate charts. */
    @Transactional(readOnly = true)
    public Map<String, ChartData> charts() {
        SecurityUser me = currentUserService.require();
        RoleCode role = me.getRoleCode();
        Map<String, ChartData> charts = new LinkedHashMap<>();
        boolean securityOps = role == RoleCode.SECURITY_ANALYST || role == RoleCode.SECURITY_MANAGER;
        if (securityOps) {
            charts.put("vulnerabilitySeverity", fromEnumCounts("Open vulnerabilities by severity",
                    assetVulnerabilityRepository.countBySeverity(OPEN_VULN), Severity.values(), true));
            charts.put("incidentStatus", fromEnumCounts("Incidents by status", incidentRepository.countByStatus(),
                    IncidentStatus.values(), false));
        }
        if (role != RoleCode.IT_INFRASTRUCTURE_ENGINEER) {
            charts.put("assetDistribution", fromRows("Assets by type", assetRepository.countByType()));
            charts.put("riskDistribution", fromEnumCounts("Open risks by level", riskRepository.countOpenByLevel(),
                    RiskLevel.values(), false));
        }
        if (role != RoleCode.COMPLIANCE_AUDITOR) {
            Long engineerId = role == RoleCode.IT_INFRASTRUCTURE_ENGINEER ? me.getId() : null;
            charts.put("remediationProgress", fromEnumCounts("Remediation tasks by status",
                    taskRepository.countByStatus(engineerId), TaskStatus.values(), false));
        }
        return charts;
    }

    private static <E extends Enum<E> & LabeledEnum> ChartData fromEnumCounts(String title, List<Object[]> rows,
                                                                             E[] order, boolean reverse) {
        Map<E, Long> counts = new LinkedHashMap<>();
        List<E> ordered = new ArrayList<>(List.of(order));
        if (reverse) {
            java.util.Collections.reverse(ordered);
        }
        for (E e : ordered) {
            counts.put(e, 0L);
        }
        for (Object[] row : rows) {
            @SuppressWarnings("unchecked")
            E key = (E) row[0];
            counts.put(key, ((Number) row[1]).longValue());
        }
        return new ChartData(title, counts.keySet().stream().map(LabeledEnum::getLabel).toList(),
                new ArrayList<>(counts.values()));
    }

    private static ChartData fromRows(String title, List<Object[]> rows) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(((LabeledEnum) row[0]).getLabel());
            values.add(((Number) row[1]).longValue());
        }
        return new ChartData(title, labels, values);
    }

    private static List<DashboardViews.QuickAction> quickActions(RoleCode role) {
        return switch (role) {
            case SECURITY_MANAGER -> List.of(
                    new DashboardViews.QuickAction("Add user", "bi-person-plus", "/users/new"),
                    new DashboardViews.QuickAction("Add asset", "bi-plus-square", "/assets/new"),
                    new DashboardViews.QuickAction("Review evidence", "bi-patch-check", "/remediation?status=PENDING_VERIFICATION"),
                    new DashboardViews.QuickAction("Reports", "bi-file-earmark-bar-graph", "/reports"),
                    new DashboardViews.QuickAction("System health", "bi-activity", "/system"));
            case SECURITY_ANALYST -> List.of(
                    new DashboardViews.QuickAction("Search assets", "bi-search", "/assets"),
                    new DashboardViews.QuickAction("New vulnerability", "bi-bug", "/vulnerabilities/new"),
                    new DashboardViews.QuickAction("Record alert", "bi-bell", "/alerts/new"),
                    new DashboardViews.QuickAction("New incident", "bi-fire", "/incidents/new"),
                    new DashboardViews.QuickAction("Review evidence", "bi-patch-check", "/remediation?status=PENDING_VERIFICATION"));
            case IT_INFRASTRUCTURE_ENGINEER -> List.of(
                    new DashboardViews.QuickAction("My assigned tasks", "bi-clipboard-check", "/remediation"),
                    new DashboardViews.QuickAction("Overdue tasks", "bi-alarm", "/remediation?overdue=true"),
                    new DashboardViews.QuickAction("My corrective actions", "bi-list-check", "/audits/actions"),
                    new DashboardViews.QuickAction("Notifications", "bi-bell", "/notifications"));
            case COMPLIANCE_AUDITOR -> List.of(
                    new DashboardViews.QuickAction("New audit plan", "bi-clipboard-plus", "/audits/new"),
                    new DashboardViews.QuickAction("Findings", "bi-journal-check", "/audits/findings"),
                    new DashboardViews.QuickAction("Evidence to review", "bi-file-earmark-check", "/audits/actions?status=PENDING_REVIEW"),
                    new DashboardViews.QuickAction("Audit report", "bi-file-earmark-pdf", "/reports"));
        };
    }
}

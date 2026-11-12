package com.cybershield.service;

import com.cybershield.entity.Asset;
import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.AuditFinding;
import com.cybershield.entity.Incident;
import com.cybershield.entity.RemediationTask;
import com.cybershield.entity.RiskAssessment;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.LabeledEnum;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.TaskStatus;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.report.CsvReportWriter;
import com.cybershield.report.PdfReportWriter;
import com.cybershield.report.ReportData;
import com.cybershield.report.ReportFilter;
import com.cybershield.report.ReportType;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.AuditFindingRepository;
import com.cybershield.repository.AuditPlanRepository;
import com.cybershield.repository.CorrectiveActionRepository;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.SecurityUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates CSV and PDF reports from live MySQL records. Access is checked per report type and role;
 * engineers only get their own remediation tasks.
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AssetRepository assetRepository;
    private final AssetVulnerabilityRepository assetVulnerabilityRepository;
    private final IncidentRepository incidentRepository;
    private final RemediationTaskRepository taskRepository;
    private final AuditFindingRepository findingRepository;
    private final AuditPlanRepository planRepository;
    private final CorrectiveActionRepository actionRepository;
    private final RiskAssessmentRepository riskRepository;
    private final DepartmentRepository departmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public ReportService(AssetRepository assetRepository, AssetVulnerabilityRepository assetVulnerabilityRepository,
                         IncidentRepository incidentRepository, RemediationTaskRepository taskRepository,
                         AuditFindingRepository findingRepository, AuditPlanRepository planRepository,
                         CorrectiveActionRepository actionRepository, RiskAssessmentRepository riskRepository,
                         DepartmentRepository departmentRepository, JdbcTemplate jdbcTemplate,
                         AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.assetRepository = assetRepository;
        this.assetVulnerabilityRepository = assetVulnerabilityRepository;
        this.incidentRepository = incidentRepository;
        this.taskRepository = taskRepository;
        this.findingRepository = findingRepository;
        this.planRepository = planRepository;
        this.actionRepository = actionRepository;
        this.riskRepository = riskRepository;
        this.departmentRepository = departmentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    public List<ReportType> availableReports() {
        RoleCode role = currentUserService.require().getRoleCode();
        return List.of(ReportType.values()).stream().filter(t -> t.isAllowedFor(role)).toList();
    }

    @Transactional
    public byte[] csv(ReportType type, ReportFilter filter) {
        ReportData data = generate(type, filter);
        logExport(type, "CSV", data.rows().size());
        return CsvReportWriter.write(data);
    }

    @Transactional
    public byte[] pdf(ReportType type, ReportFilter filter) {
        ReportData data = generate(type, filter);
        logExport(type, "PDF", data.rows().size());
        return PdfReportWriter.write(data);
    }

    @Transactional(readOnly = true)
    public ReportData generate(ReportType type, ReportFilter f) {
        SecurityUser me = currentUserService.require();
        if (!type.isAllowedFor(me.getRoleCode())) {
            throw new AccessDeniedException("Your role cannot generate the " + type.getTitle() + ".");
        }
        List<String> filters = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        List<String> headers;
        float[] weights;
        switch (type) {
            case ASSETS -> {
                headers = List.of("Code", "Name", "Type", "Department", "Office", "Owner", "Criticality", "Environment",
                        "Status", "Open findings", "Open critical", "Highest CVSS");
                weights = new float[]{1.1f, 2.6f, 1.3f, 1.6f, 1.8f, 1.4f, 1f, 1.2f, 1f, 0.9f, 0.9f, 0.9f};
                if (f.getDepartmentId() != null) {
                    departmentRepository.findById(f.getDepartmentId()).ifPresent(d -> filters.add("Department = " + d.getName()));
                }
                addFilter(filters, "Status", f.getAssetStatus());
                // Uses the v_asset_vulnerability_summary VIEW (JOIN + GROUP BY in MySQL)
                Map<Long, Object[]> summary = new HashMap<>();
                jdbcTemplate.query("SELECT asset_id, open_findings, open_critical_findings, highest_cvss "
                        + "FROM v_asset_vulnerability_summary", rs -> {
                    summary.put(rs.getLong(1), new Object[]{rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4)});
                });
                for (Asset a : assetRepository.findForReport(f.getDepartmentId(), f.getAssetStatus())) {
                    Object[] s = summary.getOrDefault(a.getId(), new Object[]{0L, 0L, null});
                    rows.add(List.of(a.getAssetCode(), a.getName(), a.getAssetType().getLabel(), a.getDepartment().getName(),
                            a.getLocation().getName(), str(ViewMapper.nameOf(a.getOwner())), a.getCriticality().getLabel(),
                            a.getEnvironment().getLabel(), a.getStatus().getLabel(), String.valueOf(s[0]),
                            String.valueOf(s[1]), s[2] == null ? "-" : s[2].toString()));
                }
            }
            case VULNERABILITIES -> {
                headers = List.of("Vulnerability", "Title", "Severity", "CVSS", "Asset", "Department", "Status",
                        "Discovered", "Last observed");
                weights = new float[]{1f, 3.6f, 0.9f, 0.6f, 1f, 1.4f, 1f, 1f, 1f};
                addFilter(filters, "Severity", f.getSeverity());
                addFilter(filters, "Status", f.getVulnerabilityStatus());
                List<AssetVulnerability> list = new ArrayList<>(assetVulnerabilityRepository.findForReport(f.getSeverity(),
                        f.getVulnerabilityStatus()));
                list.sort(Comparator.comparing((AssetVulnerability av) -> av.getVulnerability().getSeverity()).reversed()
                        .thenComparing(av -> av.getAsset().getAssetCode()));
                for (AssetVulnerability av : list) {
                    rows.add(List.of(av.getVulnerability().getVulnCode(), av.getVulnerability().getTitle(),
                            av.getVulnerability().getSeverity().getLabel(), av.getVulnerability().getCvssScore().toString(),
                            av.getAsset().getAssetCode(), av.getAsset().getDepartment().getName(), av.getStatus().getLabel(),
                            date(av.getDiscoveredDate()), date(av.getLastObservedDate())));
                }
            }
            case INCIDENTS -> {
                headers = List.of("Incident", "Title", "Type", "Severity", "Status", "Investigator", "Detected", "Resolved");
                weights = new float[]{0.9f, 3.2f, 1.4f, 0.9f, 1f, 1.4f, 1.2f, 1.2f};
                addFilter(filters, "Status", f.getIncidentStatus());
                if (f.getFrom() != null) {
                    filters.add("Detected from " + f.getFrom());
                }
                if (f.getTo() != null) {
                    filters.add("Detected to " + f.getTo());
                }
                LocalDateTime from = f.getFrom() == null ? null : f.getFrom().atStartOfDay();
                LocalDateTime to = f.getTo() == null ? null : f.getTo().plusDays(1).atStartOfDay();
                for (Incident i : incidentRepository.findForReport(f.getIncidentStatus(), from, to)) {
                    rows.add(List.of(i.getIncidentCode(), i.getTitle(), i.getIncidentType().getLabel(),
                            i.getSeverity().getLabel(), i.getStatus().getLabel(), str(ViewMapper.nameOf(i.getAssignedAnalyst())),
                            dateTime(i.getDetectedAt()), dateTime(i.getResolvedAt())));
                }
            }
            case REMEDIATION -> {
                headers = List.of("Task", "Title", "Asset", "Vulnerability", "Severity", "Priority", "Status", "Engineer",
                        "Due date", "Completed", "Overdue");
                weights = new float[]{0.9f, 2.8f, 0.9f, 1f, 0.9f, 0.9f, 1.3f, 1.3f, 1f, 1.2f, 0.7f};
                addFilter(filters, "Status", f.getTaskStatus());
                Long engineerId = null;
                if (me.getRoleCode() == RoleCode.IT_INFRASTRUCTURE_ENGINEER) {
                    engineerId = me.getId();
                    filters.add("Engineer = " + me.getFullName() + " (own tasks only)");
                }
                LocalDate today = LocalDate.now();
                for (RemediationTask t : taskRepository.findForReport(f.getTaskStatus(), engineerId)) {
                    boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(today)
                            && t.getStatus() != TaskStatus.COMPLETED;
                    rows.add(List.of(t.getTaskCode(), t.getTitle(), t.getAssetVulnerability().getAsset().getAssetCode(),
                            t.getAssetVulnerability().getVulnerability().getVulnCode(),
                            t.getAssetVulnerability().getVulnerability().getSeverity().getLabel(), t.getPriority().getLabel(),
                            t.getStatus().getLabel(), str(ViewMapper.nameOf(t.getAssignedEngineer())), date(t.getDueDate()),
                            dateTime(t.getCompletedAt()), overdue ? "Yes" : "No"));
                }
            }
            case AUDIT -> {
                headers = List.of("Plan", "Finding", "Title", "Control", "Severity", "Status", "Due date",
                        "Corrective actions", "Unverified");
                weights = new float[]{0.9f, 0.9f, 3.2f, 0.9f, 0.9f, 1f, 1f, 1f, 0.9f};
                if (f.getPlanId() != null) {
                    planRepository.findById(f.getPlanId()).ifPresent(p -> filters.add("Audit plan = " + p.getPlanCode()));
                }
                for (AuditFinding af : findingRepository.findForReport(f.getPlanId())) {
                    long total = actionRepository.findByFindingId(af.getId()).size();
                    long open = actionRepository.countUnverifiedForFinding(af.getId());
                    rows.add(List.of(af.getAuditPlan().getPlanCode(), af.getFindingCode(), af.getTitle(),
                            af.getControl() == null ? "-" : af.getControl().getControlCode(), af.getSeverity().getLabel(),
                            af.getStatus().getLabel(), date(af.getDueDate()), String.valueOf(total), String.valueOf(open)));
                }
            }
            case RISKS -> {
                headers = List.of("Risk", "Title", "Asset", "Likelihood", "Impact", "Score", "Level", "Strategy", "Owner",
                        "Status");
                weights = new float[]{0.9f, 3f, 0.9f, 0.8f, 0.7f, 0.6f, 0.9f, 1f, 1.4f, 1.3f};
                addFilter(filters, "Level", f.getRiskLevel());
                addFilter(filters, "Status", f.getRiskStatus());
                filters.add("Method: score = likelihood x impact (1-25)");
                for (RiskAssessment r : riskRepository.findForReport(f.getRiskLevel(), f.getRiskStatus())) {
                    rows.add(List.of(r.getRiskCode(), r.getTitle(), r.getAsset().getAssetCode(),
                            String.valueOf(r.getLikelihood()), String.valueOf(r.getImpact()), String.valueOf(r.getRiskScore()),
                            r.getRiskLevel().getLabel(), r.getTreatmentStrategy().getLabel(),
                            str(ViewMapper.nameOf(r.getOwner())), r.getStatus().getLabel()));
                }
            }
            default -> throw new IllegalArgumentException("Unknown report " + type);
        }
        return new ReportData(type.getTitle(), LocalDateTime.now(), me.getFullName(), filters, headers, weights, rows);
    }

    private void logExport(ReportType type, String format, int rows) {
        auditLogService.log(AuditAction.REPORT_EXPORT, AuditModule.REPORTS, RecordType.REPORT, null,
                "Exported " + type.getTitle() + " as " + format + " (" + rows + " rows).");
    }

    private static void addFilter(List<String> filters, String name, LabeledEnum value) {
        if (value != null) {
            filters.add(name + " = " + value.getLabel());
        }
    }

    private static String str(String s) {
        return s == null ? "-" : s;
    }

    private static String date(LocalDate d) {
        return d == null ? "-" : d.format(DATE);
    }

    private static String dateTime(LocalDateTime d) {
        return d == null ? "-" : d.format(DATE_TIME);
    }
}

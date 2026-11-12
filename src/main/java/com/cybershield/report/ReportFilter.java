package com.cybershield.report;

import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** All report filters (each report uses the ones that apply to it). Bound from the query string. */
public class ReportFilter {

    private Long departmentId;
    private AssetStatus assetStatus;
    private Severity severity;
    private VulnerabilityStatus vulnerabilityStatus;
    private IncidentStatus incidentStatus;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private TaskStatus taskStatus;
    private Long planId;
    private RiskLevel riskLevel;
    private RiskStatus riskStatus;

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public AssetStatus getAssetStatus() { return assetStatus; }
    public void setAssetStatus(AssetStatus assetStatus) { this.assetStatus = assetStatus; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public VulnerabilityStatus getVulnerabilityStatus() { return vulnerabilityStatus; }
    public void setVulnerabilityStatus(VulnerabilityStatus vulnerabilityStatus) { this.vulnerabilityStatus = vulnerabilityStatus; }
    public IncidentStatus getIncidentStatus() { return incidentStatus; }
    public void setIncidentStatus(IncidentStatus incidentStatus) { this.incidentStatus = incidentStatus; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public TaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(TaskStatus taskStatus) { this.taskStatus = taskStatus; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public RiskStatus getRiskStatus() { return riskStatus; }
    public void setRiskStatus(RiskStatus riskStatus) { this.riskStatus = riskStatus; }
}

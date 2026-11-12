package com.cybershield.dto;

import com.cybershield.enums.AuditPlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class AuditPlanForm {

    @NotBlank(message = "Plan name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Choose a framework")
    private Long frameworkId;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Choose the assigned auditor")
    private Long assignedAuditorId;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Target completion date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetCompletionDate;

    @NotNull
    private AuditPlanStatus status = AuditPlanStatus.PLANNED;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFrameworkId() { return frameworkId; }
    public void setFrameworkId(Long frameworkId) { this.frameworkId = frameworkId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssignedAuditorId() { return assignedAuditorId; }
    public void setAssignedAuditorId(Long assignedAuditorId) { this.assignedAuditorId = assignedAuditorId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getTargetCompletionDate() { return targetCompletionDate; }
    public void setTargetCompletionDate(LocalDate targetCompletionDate) { this.targetCompletionDate = targetCompletionDate; }
    public AuditPlanStatus getStatus() { return status; }
    public void setStatus(AuditPlanStatus status) { this.status = status; }
}

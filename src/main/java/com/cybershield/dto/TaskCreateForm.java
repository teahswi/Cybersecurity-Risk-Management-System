package com.cybershield.dto;

import com.cybershield.enums.Priority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Creates a remediation task for one asset vulnerability; optionally assigns it straight away. */
public class TaskCreateForm {

    @NotNull
    private Long assetVulnerabilityId;

    @NotBlank(message = "Task title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Describe the required action")
    @Size(max = 5000)
    private String description;

    @NotNull(message = "Choose a priority")
    private Priority priority;

    /** Optional: when set, the task is assigned (status ASSIGNED) in the same transaction. */
    private Long engineerId;

    @FutureOrPresent(message = "The deadline cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    public Long getAssetVulnerabilityId() { return assetVulnerabilityId; }
    public void setAssetVulnerabilityId(Long assetVulnerabilityId) { this.assetVulnerabilityId = assetVulnerabilityId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Long getEngineerId() { return engineerId; }
    public void setEngineerId(Long engineerId) { this.engineerId = engineerId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}

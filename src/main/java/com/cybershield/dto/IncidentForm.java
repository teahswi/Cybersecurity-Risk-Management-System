package com.cybershield.dto;

import com.cybershield.enums.IncidentType;
import com.cybershield.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Creates or edits an incident. alertId (optional) links the alert the incident was raised from. */
public class IncidentForm {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000)
    private String description;

    @NotNull(message = "Choose an incident type")
    private IncidentType incidentType;

    @NotNull(message = "Choose a severity")
    private Severity severity;

    private Long assignedAnalystId;

    @NotNull(message = "Detection time is required")
    @PastOrPresent(message = "Detection time cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime detectedAt = LocalDateTime.now().withSecond(0).withNano(0);

    private Long alertId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IncidentType getIncidentType() { return incidentType; }
    public void setIncidentType(IncidentType incidentType) { this.incidentType = incidentType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Long getAssignedAnalystId() { return assignedAnalystId; }
    public void setAssignedAnalystId(Long assignedAnalystId) { this.assignedAnalystId = assignedAnalystId; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
}

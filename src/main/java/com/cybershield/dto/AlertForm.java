package com.cybershield.dto;

import com.cybershield.enums.AlertSource;
import com.cybershield.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Records a security alert (entered manually - CyberShield is not connected to a live SIEM). */
public class AlertForm {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Choose the alert source")
    private AlertSource source;

    @NotNull(message = "Choose a severity")
    private Severity severity;

    private Long assetId;

    @NotNull(message = "Detection time is required")
    @PastOrPresent(message = "Detection time cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime detectedAt = LocalDateTime.now().withSecond(0).withNano(0);

    private Long assignedAnalystId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AlertSource getSource() { return source; }
    public void setSource(AlertSource source) { this.source = source; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public Long getAssignedAnalystId() { return assignedAnalystId; }
    public void setAssignedAnalystId(Long assignedAnalystId) { this.assignedAnalystId = assignedAnalystId; }
}

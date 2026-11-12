package com.cybershield.dto;

import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.TreatmentStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Risk assessment form. Note: there is deliberately NO score or level field - the server always
 * calculates Risk Score = Likelihood x Impact and never trusts a value from the browser.
 */
public class RiskForm {

    @NotBlank(message = "Risk title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Choose the related asset")
    private Long assetId;

    private Long assetVulnerabilityId;

    @NotNull(message = "Likelihood is required")
    @Min(value = 1, message = "Likelihood must be between 1 and 5")
    @Max(value = 5, message = "Likelihood must be between 1 and 5")
    private Integer likelihood;

    @NotNull(message = "Impact is required")
    @Min(value = 1, message = "Impact must be between 1 and 5")
    @Max(value = 5, message = "Impact must be between 1 and 5")
    private Integer impact;

    @NotNull(message = "Choose a treatment strategy")
    private TreatmentStrategy treatmentStrategy;

    private Long ownerId;

    @NotNull(message = "Choose a status")
    private RiskStatus status = RiskStatus.OPEN;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public Long getAssetVulnerabilityId() { return assetVulnerabilityId; }
    public void setAssetVulnerabilityId(Long assetVulnerabilityId) { this.assetVulnerabilityId = assetVulnerabilityId; }
    public Integer getLikelihood() { return likelihood; }
    public void setLikelihood(Integer likelihood) { this.likelihood = likelihood; }
    public Integer getImpact() { return impact; }
    public void setImpact(Integer impact) { this.impact = impact; }
    public TreatmentStrategy getTreatmentStrategy() { return treatmentStrategy; }
    public void setTreatmentStrategy(TreatmentStrategy treatmentStrategy) { this.treatmentStrategy = treatmentStrategy; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public RiskStatus getStatus() { return status; }
    public void setStatus(RiskStatus status) { this.status = status; }
}

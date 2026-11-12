package com.cybershield.dto;

import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.Criticality;
import com.cybershield.enums.Environment;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.LicenseType;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only view objects for the asset pages (built inside a transaction, safe to render). */
public final class AssetViews {

    private AssetViews() {
    }

    public record Row(Long id, String assetCode, String name, AssetType assetType, String hostname, String ipAddress,
                      String departmentName, String locationName, String ownerName, Criticality criticality,
                      Environment environment, AssetStatus status) {
    }

    public record Detail(Long id, String assetCode, String name, AssetType assetType, String hostname, String ipAddress,
                         String operatingSystem, String departmentName, String locationName, String ownerName,
                         Criticality criticality, Environment environment, AssetStatus status, String description,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {

        public boolean isArchived() {
            return status == AssetStatus.ARCHIVED;
        }
    }

    public record SoftwareInstall(Long id, Long softwareId, String softwareName, String vendor, String installedVersion,
                                  String baselineVersion, boolean outdated, LocalDate installationDate,
                                  LicenseType licenseType, LocalDate licenseExpiryDate, boolean licenseExpired) {
    }

    public record VulnerabilityInstance(Long id, Long vulnerabilityId, String vulnCode, String title, Severity severity,
                                        BigDecimal cvssScore, VulnerabilityStatus status, LocalDate discoveredDate,
                                        LocalDate lastObservedDate) {
    }

    public record TaskLine(Long id, String taskCode, String title, String vulnCode, TaskStatus status,
                           String engineerName, LocalDate dueDate, LocalDateTime completedAt) {
    }

    public record IncidentLine(Long id, String incidentCode, String title, Severity severity, IncidentStatus status,
                               LocalDateTime detectedAt, String impact) {
    }

    public record AlertLine(Long id, String alertCode, String title, Severity severity, AlertStatus status,
                            LocalDateTime detectedAt) {
    }

    public record RiskLine(Long id, String riskCode, String title, Integer riskScore, RiskLevel riskLevel,
                           RiskStatus status) {
    }

    /** Everything shown on the asset details page. */
    public record DetailPage(Detail asset, List<SoftwareInstall> software, List<VulnerabilityInstance> vulnerabilities,
                       List<TaskLine> tasks, List<IncidentLine> incidents, List<AlertLine> alerts,
                       List<RiskLine> risks, List<AuditLogView> history) {
    }
}

package com.cybershield.dto;

import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.TreatmentStatus;
import com.cybershield.enums.TreatmentStrategy;
import com.cybershield.enums.VulnerabilityStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class RiskViews {

    private RiskViews() {
    }

    public record Row(Long id, String riskCode, String title, String assetCode, int likelihood, int impact, int riskScore,
                      RiskLevel riskLevel, TreatmentStrategy treatmentStrategy, String ownerName, RiskStatus status) {
    }

    public record Treatment(Long id, TreatmentStrategy strategy, String actionDescription, String ownerName,
                            LocalDate deadline, TreatmentStatus status, boolean overdue) {
    }

    public record Detail(Long id, String riskCode, String title, String description, Long assetId, String assetCode,
                         String assetName, Long assetVulnerabilityId, String vulnCode, VulnerabilityStatus vulnStatus,
                         int likelihood, int impact, int riskScore, RiskLevel riskLevel,
                         TreatmentStrategy treatmentStrategy, String ownerName, RiskStatus status, String createdByName,
                         LocalDateTime createdAt, LocalDateTime updatedAt, List<Treatment> treatments,
                         List<AuditLogView> history) {
    }

    /** Count of open risks in each cell of the 5x5 likelihood/impact matrix. */
    public record Matrix(long[][] cells) {

        public long count(int likelihood, int impact) {
            return cells[likelihood - 1][impact - 1];
        }
    }
}

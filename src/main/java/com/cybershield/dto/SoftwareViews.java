package com.cybershield.dto;

import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.LicenseType;
import com.cybershield.enums.SoftwareCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SoftwareViews {

    private SoftwareViews() {
    }

    public record Row(Long id, String name, String vendor, SoftwareCategory category, String baselineVersion,
                      long installations) {
    }

    public record Installation(Long id, Long assetId, String assetCode, String assetName, String departmentName,
                               AssetStatus assetStatus, String installedVersion, boolean outdated,
                               LocalDate installationDate, LicenseType licenseType, LocalDate licenseExpiryDate) {
    }

    public record Detail(Long id, String name, String vendor, SoftwareCategory category, String baselineVersion,
                         String description, LocalDateTime createdAt, LocalDateTime updatedAt,
                         List<Installation> installations, long outdatedCount) {
    }
}

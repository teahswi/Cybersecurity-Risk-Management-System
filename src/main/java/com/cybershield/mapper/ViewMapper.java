package com.cybershield.mapper;

import com.cybershield.dto.AssetViews;
import com.cybershield.entity.Asset;
import com.cybershield.entity.AssetSoftware;
import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.RemediationTask;
import com.cybershield.entity.User;
import com.cybershield.util.VersionComparator;
import java.time.LocalDate;

/**
 * Converts entities to read-only view records. Always called inside a service transaction,
 * because spring.jpa.open-in-view=false (lazy associations cannot be loaded later in the view).
 */
public final class ViewMapper {

    private ViewMapper() {
    }

    public static String nameOf(User user) {
        return user == null ? null : user.getFullName();
    }

    public static AssetViews.Row assetRow(Asset a) {
        return new AssetViews.Row(a.getId(), a.getAssetCode(), a.getName(), a.getAssetType(), a.getHostname(),
                a.getIpAddress(), a.getDepartment().getName(), a.getLocation().getName(), nameOf(a.getOwner()),
                a.getCriticality(), a.getEnvironment(), a.getStatus());
    }

    public static AssetViews.Detail assetDetail(Asset a) {
        return new AssetViews.Detail(a.getId(), a.getAssetCode(), a.getName(), a.getAssetType(), a.getHostname(),
                a.getIpAddress(), a.getOperatingSystem(), a.getDepartment().getName(),
                a.getLocation().getName() + ", " + a.getLocation().getCity(), nameOf(a.getOwner()), a.getCriticality(),
                a.getEnvironment(), a.getStatus(), a.getDescription(), a.getCreatedAt(), a.getUpdatedAt());
    }

    public static AssetViews.SoftwareInstall install(AssetSoftware x) {
        boolean expired = x.getLicenseExpiryDate() != null && x.getLicenseExpiryDate().isBefore(LocalDate.now());
        return new AssetViews.SoftwareInstall(x.getId(), x.getSoftware().getId(), x.getSoftware().getName(),
                x.getSoftware().getVendor(), x.getInstalledVersion(), x.getSoftware().getBaselineVersion(),
                VersionComparator.isOutdated(x.getInstalledVersion(), x.getSoftware().getBaselineVersion()),
                x.getInstallationDate(), x.getLicenseType(), x.getLicenseExpiryDate(), expired);
    }

    public static AssetViews.VulnerabilityInstance vulnerabilityInstance(AssetVulnerability av) {
        return new AssetViews.VulnerabilityInstance(av.getId(), av.getVulnerability().getId(),
                av.getVulnerability().getVulnCode(), av.getVulnerability().getTitle(), av.getVulnerability().getSeverity(),
                av.getVulnerability().getCvssScore(), av.getStatus(), av.getDiscoveredDate(), av.getLastObservedDate());
    }

    public static AssetViews.TaskLine taskLine(RemediationTask t) {
        return new AssetViews.TaskLine(t.getId(), t.getTaskCode(), t.getTitle(),
                t.getAssetVulnerability().getVulnerability().getVulnCode(), t.getStatus(),
                nameOf(t.getAssignedEngineer()), t.getDueDate(), t.getCompletedAt());
    }
}

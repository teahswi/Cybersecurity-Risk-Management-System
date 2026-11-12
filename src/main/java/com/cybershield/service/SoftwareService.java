package com.cybershield.service;

import com.cybershield.dto.InstallSoftwareForm;
import com.cybershield.dto.SoftwareForm;
import com.cybershield.dto.SoftwareViews;
import com.cybershield.entity.Asset;
import com.cybershield.entity.AssetSoftware;
import com.cybershield.entity.Software;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.SoftwareCategory;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetSoftwareRepository;
import com.cybershield.repository.SoftwareRepository;
import com.cybershield.security.Roles;
import com.cybershield.util.Texts;
import com.cybershield.util.VersionComparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Software catalogue and installations (asset_software). Changes are made by the Security Manager. */
@Service
public class SoftwareService {

    private final SoftwareRepository softwareRepository;
    private final AssetSoftwareRepository assetSoftwareRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;

    public SoftwareService(SoftwareRepository softwareRepository, AssetSoftwareRepository assetSoftwareRepository,
                           AssetRepository assetRepository, AuditLogService auditLogService) {
        this.softwareRepository = softwareRepository;
        this.assetSoftwareRepository = assetSoftwareRepository;
        this.assetRepository = assetRepository;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<SoftwareViews.Row> search(String q, SoftwareCategory category, Pageable pageable) {
        Page<Software> page = softwareRepository.search(Texts.clean(q), category, pageable);
        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(Software::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : assetSoftwareRepository.countInstallations(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return page.map(s -> new SoftwareViews.Row(s.getId(), s.getName(), s.getVendor(), s.getCategory(),
                s.getBaselineVersion(), counts.getOrDefault(s.getId(), 0L)));
    }

    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public SoftwareViews.Detail detail(Long id) {
        Software s = find(id);
        List<SoftwareViews.Installation> installs = assetSoftwareRepository.findBySoftwareId(id).stream()
                .map(x -> new SoftwareViews.Installation(x.getId(), x.getAsset().getId(), x.getAsset().getAssetCode(),
                        x.getAsset().getName(), x.getAsset().getDepartment().getName(), x.getAsset().getStatus(),
                        x.getInstalledVersion(), VersionComparator.isOutdated(x.getInstalledVersion(), s.getBaselineVersion()),
                        x.getInstallationDate(), x.getLicenseType(), x.getLicenseExpiryDate()))
                .toList();
        long outdated = installs.stream().filter(SoftwareViews.Installation::outdated).count();
        return new SoftwareViews.Detail(s.getId(), s.getName(), s.getVendor(), s.getCategory(), s.getBaselineVersion(),
                s.getDescription(), s.getCreatedAt(), s.getUpdatedAt(), installs, outdated);
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public SoftwareForm formFor(Long id) {
        Software s = find(id);
        SoftwareForm f = new SoftwareForm();
        f.setName(s.getName());
        f.setVendor(s.getVendor());
        f.setCategory(s.getCategory());
        f.setBaselineVersion(s.getBaselineVersion());
        f.setDescription(s.getDescription());
        return f;
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public Long create(SoftwareForm form) {
        if (softwareRepository.existsByNameIgnoreCaseAndVendorIgnoreCase(form.getName().trim(), form.getVendor().trim())) {
            throw new BusinessRuleException("This software product already exists for that vendor.");
        }
        Software s = new Software();
        apply(s, form);
        softwareRepository.save(s);
        auditLogService.log(AuditAction.CREATE, AuditModule.SOFTWARE, RecordType.SOFTWARE, s.getId(),
                "Added software " + s.getName() + " (" + s.getVendor() + ").");
        return s.getId();
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void update(Long id, SoftwareForm form) {
        Software s = find(id);
        if (softwareRepository.existsByNameIgnoreCaseAndVendorIgnoreCaseAndIdNot(form.getName().trim(),
                form.getVendor().trim(), id)) {
            throw new BusinessRuleException("This software product already exists for that vendor.");
        }
        apply(s, form);
        auditLogService.log(AuditAction.UPDATE, AuditModule.SOFTWARE, RecordType.SOFTWARE, id,
                "Updated software " + s.getName() + ".");
    }

    /** Records that a software product is installed on an asset. */
    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void install(InstallSoftwareForm form) {
        Asset asset = assetRepository.findById(form.getAssetId())
                .orElseThrow(() -> ResourceNotFoundException.of("Asset", form.getAssetId()));
        if (asset.getStatus() == AssetStatus.ARCHIVED) {
            throw new BusinessRuleException("Software cannot be linked to an archived asset.");
        }
        Software software = find(form.getSoftwareId());
        if (assetSoftwareRepository.existsByAssetIdAndSoftwareId(asset.getId(), software.getId())) {
            throw new BusinessRuleException(software.getName() + " is already recorded on " + asset.getAssetCode()
                    + ". Remove the existing entry first to change its version.");
        }
        if (form.getLicenseExpiryDate() != null && form.getInstallationDate() != null
                && form.getLicenseExpiryDate().isBefore(form.getInstallationDate())) {
            throw new BusinessRuleException("Licence expiry date cannot be before the installation date.");
        }
        AssetSoftware x = new AssetSoftware();
        x.setAsset(asset);
        x.setSoftware(software);
        x.setInstalledVersion(form.getInstalledVersion().trim());
        x.setInstallationDate(form.getInstallationDate());
        x.setLicenseType(form.getLicenseType());
        x.setLicenseExpiryDate(form.getLicenseExpiryDate());
        assetSoftwareRepository.save(x);
        auditLogService.log(AuditAction.LINK, AuditModule.SOFTWARE, RecordType.ASSET, asset.getId(),
                "Linked " + software.getName() + " " + x.getInstalledVersion() + " to asset " + asset.getAssetCode() + ".");
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public Long uninstall(Long assetSoftwareId) {
        AssetSoftware x = assetSoftwareRepository.findById(assetSoftwareId)
                .orElseThrow(() -> ResourceNotFoundException.of("Installation", assetSoftwareId));
        Long assetId = x.getAsset().getId();
        String text = "Removed " + x.getSoftware().getName() + " " + x.getInstalledVersion() + " from asset "
                + x.getAsset().getAssetCode() + ".";
        assetSoftwareRepository.delete(x);
        auditLogService.log(AuditAction.UNLINK, AuditModule.SOFTWARE, RecordType.ASSET, assetId, text);
        return assetId;
    }

    private Software find(Long id) {
        return softwareRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Software", id));
    }

    private void apply(Software s, SoftwareForm form) {
        s.setName(form.getName().trim());
        s.setVendor(form.getVendor().trim());
        s.setCategory(form.getCategory());
        s.setBaselineVersion(Texts.clean(form.getBaselineVersion()));
        s.setDescription(Texts.clean(form.getDescription()));
    }
}

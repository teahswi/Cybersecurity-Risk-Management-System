package com.cybershield.service;

import com.cybershield.dto.AssetFilter;
import com.cybershield.dto.AssetForm;
import com.cybershield.dto.AssetViews;
import com.cybershield.entity.Asset;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetSoftwareRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.IncidentAssetRepository;
import com.cybershield.repository.OfficeLocationRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.util.Texts;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asset inventory. Everyone can view assets, but IT engineers only see assets they own or have
 * remediation tasks on. Only the Security Manager can add, edit, archive or restore assets.
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficeLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AssetSoftwareRepository assetSoftwareRepository;
    private final AssetVulnerabilityRepository assetVulnerabilityRepository;
    private final RemediationTaskRepository taskRepository;
    private final IncidentAssetRepository incidentAssetRepository;
    private final SecurityAlertRepository alertRepository;
    private final RiskAssessmentRepository riskRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AssetService(AssetRepository assetRepository, DepartmentRepository departmentRepository,
                        OfficeLocationRepository locationRepository, UserRepository userRepository,
                        AssetSoftwareRepository assetSoftwareRepository,
                        AssetVulnerabilityRepository assetVulnerabilityRepository, RemediationTaskRepository taskRepository,
                        IncidentAssetRepository incidentAssetRepository, SecurityAlertRepository alertRepository,
                        RiskAssessmentRepository riskRepository, AuditLogService auditLogService,
                        CurrentUserService currentUserService) {
        this.assetRepository = assetRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.assetSoftwareRepository = assetSoftwareRepository;
        this.assetVulnerabilityRepository = assetVulnerabilityRepository;
        this.taskRepository = taskRepository;
        this.incidentAssetRepository = incidentAssetRepository;
        this.alertRepository = alertRepository;
        this.riskRepository = riskRepository;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional(readOnly = true)
    public Page<AssetViews.Row> search(AssetFilter f, Pageable pageable) {
        Long restrictTo = currentUserService.isEngineer() ? currentUserService.requireId() : null;
        return assetRepository.search(Texts.clean(f.getQ()), f.getDepartmentId(), f.getLocationId(), f.getType(),
                f.getEnvironment(), f.getStatus(), restrictTo, pageable).map(ViewMapper::assetRow);
    }

    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional(readOnly = true)
    public AssetViews.DetailPage detailPage(Long id) {
        Asset asset = assetRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Asset", id));
        checkCanView(asset.getId());
        boolean securityOps = currentUserService.isAnalyst() || currentUserService.isManager();
        boolean auditTeam = currentUserService.isAuditor() || currentUserService.isManager();
        List<AssetViews.SoftwareInstall> software = assetSoftwareRepository.findByAssetId(id).stream()
                .map(ViewMapper::install).toList();
        List<AssetViews.VulnerabilityInstance> vulns = currentUserService.isAuditor() ? List.of()
                : assetVulnerabilityRepository.findByAssetId(id).stream().map(ViewMapper::vulnerabilityInstance).toList();
        List<AssetViews.TaskLine> tasks = currentUserService.isAuditor() ? List.of()
                : taskRepository.findByAssetId(id).stream().map(ViewMapper::taskLine).toList();
        List<AssetViews.IncidentLine> incidents = !securityOps ? List.of()
                : incidentAssetRepository.findByAssetId(id).stream().map(x -> new AssetViews.IncidentLine(
                        x.getIncident().getId(), x.getIncident().getIncidentCode(), x.getIncident().getTitle(),
                        x.getIncident().getSeverity(), x.getIncident().getStatus(), x.getIncident().getDetectedAt(),
                        x.getImpactDescription())).toList();
        List<AssetViews.AlertLine> alerts = !securityOps ? List.of()
                : alertRepository.findByAssetId(id).stream().map(s -> new AssetViews.AlertLine(s.getId(),
                        s.getAlertCode(), s.getTitle(), s.getSeverity(), s.getStatus(), s.getDetectedAt())).toList();
        List<AssetViews.RiskLine> risks = !(securityOps || auditTeam) ? List.of()
                : riskRepository.findByAssetId(id).stream().map(r -> new AssetViews.RiskLine(r.getId(), r.getRiskCode(),
                        r.getTitle(), r.getRiskScore(), r.getRiskLevel(), r.getStatus())).toList();
        return new AssetViews.DetailPage(ViewMapper.assetDetail(asset), software, vulns, tasks, incidents, alerts, risks,
                auditLogService.historyOf(RecordType.ASSET, id));
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public AssetForm formFor(Long id) {
        Asset a = findAsset(id);
        AssetForm form = new AssetForm();
        form.setAssetCode(a.getAssetCode());
        form.setName(a.getName());
        form.setAssetType(a.getAssetType());
        form.setHostname(a.getHostname());
        form.setIpAddress(a.getIpAddress());
        form.setOperatingSystem(a.getOperatingSystem());
        form.setDepartmentId(a.getDepartment().getId());
        form.setLocationId(a.getLocation().getId());
        form.setOwnerId(a.getOwner() == null ? null : a.getOwner().getId());
        form.setCriticality(a.getCriticality());
        form.setEnvironment(a.getEnvironment());
        form.setStatus(a.getStatus());
        form.setDescription(a.getDescription());
        return form;
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public Long create(AssetForm form) {
        String code = form.getAssetCode().trim().toUpperCase();
        if (assetRepository.existsByAssetCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Asset code " + code + " is already used by another asset.");
        }
        Asset asset = new Asset();
        apply(asset, form);
        asset.setAssetCode(code);
        assetRepository.save(asset);
        auditLogService.log(AuditAction.CREATE, AuditModule.ASSETS, RecordType.ASSET, asset.getId(),
                "Created asset " + code + " (" + asset.getName() + ").");
        return asset.getId();
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void update(Long id, AssetForm form) {
        Asset asset = findAsset(id);
        String code = form.getAssetCode().trim().toUpperCase();
        if (assetRepository.existsByAssetCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException("Asset code " + code + " is already used by another asset.");
        }
        AssetStatus oldStatus = asset.getStatus();
        apply(asset, form);
        asset.setAssetCode(code);
        auditLogService.log(AuditAction.UPDATE, AuditModule.ASSETS, RecordType.ASSET, id,
                "Updated asset " + code + (oldStatus != asset.getStatus()
                        ? " (status " + oldStatus.getLabel() + " -> " + asset.getStatus().getLabel() + ")" : "") + ".");
    }

    /** Assets are never physically deleted because history (tasks, incidents, risks) depends on them. */
    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void archive(Long id) {
        Asset asset = findAsset(id);
        if (asset.getStatus() == AssetStatus.ARCHIVED) {
            throw new BusinessRuleException("Asset " + asset.getAssetCode() + " is already archived.");
        }
        asset.setStatus(AssetStatus.ARCHIVED);
        auditLogService.log(AuditAction.ARCHIVE, AuditModule.ASSETS, RecordType.ASSET, id,
                "Archived asset " + asset.getAssetCode() + ".");
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void restore(Long id) {
        Asset asset = findAsset(id);
        if (asset.getStatus() != AssetStatus.ARCHIVED) {
            throw new BusinessRuleException("Only archived assets can be restored.");
        }
        asset.setStatus(AssetStatus.ACTIVE);
        auditLogService.log(AuditAction.RESTORE, AuditModule.ASSETS, RecordType.ASSET, id,
                "Restored asset " + asset.getAssetCode() + " to Active.");
    }

    /** Engineers may only open assets that are related to them. */
    @Transactional(readOnly = true)
    public void checkCanView(Long assetId) {
        if (currentUserService.isEngineer()
                && assetRepository.countRelatedToEngineer(assetId, currentUserService.requireId()) == 0) {
            throw new AccessDeniedException("Engineers can only view assets related to their own tasks.");
        }
    }

    private Asset findAsset(Long id) {
        return assetRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Asset", id));
    }

    private void apply(Asset asset, AssetForm form) {
        asset.setName(form.getName().trim());
        asset.setAssetType(form.getAssetType());
        asset.setHostname(Texts.clean(form.getHostname()));
        asset.setIpAddress(Texts.clean(form.getIpAddress()));
        asset.setOperatingSystem(Texts.clean(form.getOperatingSystem()));
        asset.setDepartment(departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid department.")));
        asset.setLocation(locationRepository.findById(form.getLocationId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid office location.")));
        asset.setOwner(form.getOwnerId() == null ? null : userRepository.findById(form.getOwnerId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid asset owner.")));
        asset.setCriticality(form.getCriticality());
        asset.setEnvironment(form.getEnvironment());
        asset.setStatus(form.getStatus());
        asset.setDescription(Texts.clean(form.getDescription()));
    }
}

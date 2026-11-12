package com.cybershield.service;

import com.cybershield.dto.RiskForm;
import com.cybershield.dto.RiskViews;
import com.cybershield.dto.TreatmentForm;
import com.cybershield.entity.Asset;
import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.RiskAssessment;
import com.cybershield.entity.RiskTreatment;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.TreatmentStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.repository.RiskTreatmentRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.util.Codes;
import com.cybershield.util.Texts;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Risk management. Risk Score = Likelihood x Impact (each 1-5), calculated here on the server.
 * Levels: 1-4 Low, 5-9 Medium, 10-16 High, 17-25 Critical (project methodology, not an official standard).
 */
@Service
public class RiskService {

    private final RiskAssessmentRepository riskRepository;
    private final RiskTreatmentRepository treatmentRepository;
    private final AssetRepository assetRepository;
    private final AssetVulnerabilityRepository assetVulnerabilityRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public RiskService(RiskAssessmentRepository riskRepository, RiskTreatmentRepository treatmentRepository,
                       AssetRepository assetRepository, AssetVulnerabilityRepository assetVulnerabilityRepository,
                       UserRepository userRepository, AuditLogService auditLogService,
                       CurrentUserService currentUserService) {
        this.riskRepository = riskRepository;
        this.treatmentRepository = treatmentRepository;
        this.assetRepository = assetRepository;
        this.assetVulnerabilityRepository = assetVulnerabilityRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    /** The scoring formula, kept in one static method so it can be unit-tested. */
    public static int calculateScore(int likelihood, int impact) {
        if (likelihood < 1 || likelihood > 5 || impact < 1 || impact > 5) {
            throw new BusinessRuleException("Likelihood and impact must both be between 1 and 5.");
        }
        return likelihood * impact;
    }

    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<RiskViews.Row> search(String q, RiskLevel level, RiskStatus status, Pageable pageable) {
        return riskRepository.search(Texts.clean(q), level, status, pageable).map(r -> new RiskViews.Row(r.getId(),
                r.getRiskCode(), r.getTitle(), r.getAsset().getAssetCode(), r.getLikelihood(), r.getImpact(),
                r.getRiskScore(), r.getRiskLevel(), r.getTreatmentStrategy(), ViewMapper.nameOf(r.getOwner()),
                r.getStatus()));
    }

    /** Builds the 5x5 heat-map of open risks (likelihood rows x impact columns). */
    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public RiskViews.Matrix matrix() {
        long[][] cells = new long[5][5];
        for (RiskAssessment r : riskRepository.findForReport(null, null)) {
            if (r.getStatus() != RiskStatus.CLOSED) {
                cells[r.getLikelihood() - 1][r.getImpact() - 1]++;
            }
        }
        return new RiskViews.Matrix(cells);
    }

    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public RiskViews.Detail detail(Long id) {
        RiskAssessment r = riskRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Risk", id));
        LocalDate today = LocalDate.now();
        List<RiskViews.Treatment> treatments = treatmentRepository.findByRiskId(id).stream()
                .map(t -> new RiskViews.Treatment(t.getId(), t.getStrategy(), t.getActionDescription(),
                        ViewMapper.nameOf(t.getOwner()), t.getDeadline(), t.getStatus(),
                        t.getDeadline() != null && t.getDeadline().isBefore(today)
                                && (t.getStatus() == TreatmentStatus.PLANNED || t.getStatus() == TreatmentStatus.IN_PROGRESS)))
                .toList();
        AssetVulnerability av = r.getAssetVulnerability();
        return new RiskViews.Detail(r.getId(), r.getRiskCode(), r.getTitle(), r.getDescription(), r.getAsset().getId(),
                r.getAsset().getAssetCode(), r.getAsset().getName(), av == null ? null : av.getId(),
                av == null ? null : av.getVulnerability().getVulnCode(), av == null ? null : av.getStatus(),
                r.getLikelihood(), r.getImpact(), r.getRiskScore(), r.getRiskLevel(), r.getTreatmentStrategy(),
                ViewMapper.nameOf(r.getOwner()), r.getStatus(), r.getCreatedBy().getFullName(), r.getCreatedAt(),
                r.getUpdatedAt(), treatments, auditLogService.historyOf(RecordType.RISK, id));
    }

    /** Pre-fills a new risk form from an asset vulnerability (used by the "Assess risk" button). */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public RiskForm newForm(Long assetId, Long assetVulnerabilityId) {
        RiskForm form = new RiskForm();
        if (assetVulnerabilityId != null) {
            AssetVulnerability av = assetVulnerabilityRepository.findDetailById(assetVulnerabilityId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Asset vulnerability", assetVulnerabilityId));
            form.setAssetId(av.getAsset().getId());
            form.setAssetVulnerabilityId(av.getId());
            form.setTitle("Exploitation of " + av.getVulnerability().getVulnCode() + " on " + av.getAsset().getAssetCode());
        } else {
            form.setAssetId(assetId);
        }
        form.setOwnerId(currentUserService.requireId());
        return form;
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public RiskForm formFor(Long id) {
        RiskAssessment r = riskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Risk", id));
        RiskForm f = new RiskForm();
        f.setTitle(r.getTitle());
        f.setDescription(r.getDescription());
        f.setAssetId(r.getAsset().getId());
        f.setAssetVulnerabilityId(r.getAssetVulnerability() == null ? null : r.getAssetVulnerability().getId());
        f.setLikelihood(r.getLikelihood());
        f.setImpact(r.getImpact());
        f.setTreatmentStrategy(r.getTreatmentStrategy());
        f.setOwnerId(r.getOwner() == null ? null : r.getOwner().getId());
        f.setStatus(r.getStatus());
        return f;
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public Long create(RiskForm form) {
        RiskAssessment r = new RiskAssessment();
        r.setRiskCode(Codes.temporary());
        apply(r, form);
        r.setCreatedBy(currentUserService.requireEntity());
        riskRepository.save(r);
        r.setRiskCode(Codes.format("RSK", r.getId()));
        auditLogService.log(AuditAction.CREATE, AuditModule.RISKS, RecordType.RISK, r.getId(),
                "Created risk " + r.getRiskCode() + ": likelihood " + r.getLikelihood() + " x impact " + r.getImpact()
                        + " = score " + r.getRiskScore() + " (" + r.getRiskLevel().getLabel() + ").");
        return r.getId();
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void update(Long id, RiskForm form) {
        RiskAssessment r = riskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Risk", id));
        int oldScore = r.getRiskScore();
        RiskStatus oldStatus = r.getStatus();
        apply(r, form);
        auditLogService.log(AuditAction.UPDATE, AuditModule.RISKS, RecordType.RISK, id,
                "Updated risk " + r.getRiskCode() + ": score " + oldScore + " -> " + r.getRiskScore() + " ("
                        + r.getRiskLevel().getLabel() + ")"
                        + (oldStatus != r.getStatus() ? ", status " + oldStatus.getLabel() + " -> " + r.getStatus().getLabel() : "")
                        + ".");
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void addTreatment(Long riskId, TreatmentForm form) {
        RiskAssessment r = riskRepository.findById(riskId).orElseThrow(() -> ResourceNotFoundException.of("Risk", riskId));
        if (r.getStatus() == RiskStatus.CLOSED) {
            throw new BusinessRuleException("Treatments cannot be added to a closed risk.");
        }
        if (form.getDeadline() != null && form.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The treatment deadline cannot be in the past.");
        }
        RiskTreatment t = new RiskTreatment();
        t.setRisk(r);
        t.setStrategy(form.getStrategy());
        t.setActionDescription(form.getActionDescription().trim());
        t.setOwner(form.getOwnerId() == null ? null : userRepository.findById(form.getOwnerId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid owner.")));
        t.setDeadline(form.getDeadline());
        t.setStatus(TreatmentStatus.PLANNED);
        t.setCreatedBy(currentUserService.requireEntity());
        treatmentRepository.save(t);
        if (r.getStatus() == RiskStatus.OPEN) {
            r.setStatus(RiskStatus.TREATMENT_PLANNED);
        }
        auditLogService.log(AuditAction.CREATE, AuditModule.RISKS, RecordType.RISK, riskId,
                "Added " + t.getStrategy().getLabel() + " treatment to " + r.getRiskCode()
                        + (t.getDeadline() != null ? " (deadline " + t.getDeadline() + ")" : "") + ".");
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public Long updateTreatmentStatus(Long treatmentId, TreatmentStatus status) {
        RiskTreatment t = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Treatment", treatmentId));
        if (status == null) {
            throw new BusinessRuleException("Choose a status.");
        }
        TreatmentStatus old = t.getStatus();
        if (old == TreatmentStatus.COMPLETED || old == TreatmentStatus.CANCELLED) {
            throw new BusinessRuleException("A " + old.getLabel().toLowerCase() + " treatment cannot be changed.");
        }
        t.setStatus(status);
        RiskAssessment r = t.getRisk();
        if (status == TreatmentStatus.IN_PROGRESS && (r.getStatus() == RiskStatus.OPEN
                || r.getStatus() == RiskStatus.TREATMENT_PLANNED)) {
            r.setStatus(RiskStatus.IN_TREATMENT);
        }
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.RISKS, RecordType.RISK, r.getId(),
                "Treatment on " + r.getRiskCode() + ": " + old.getLabel() + " -> " + status.getLabel() + ".");
        return r.getId();
    }

    @PreAuthorize(Roles.ANALYST_AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public List<RiskViews.Row> topRisks(int limit) {
        return riskRepository.findTopOpen(PageRequest.of(0, limit)).stream().map(r -> new RiskViews.Row(r.getId(),
                r.getRiskCode(), r.getTitle(), r.getAsset().getAssetCode(), r.getLikelihood(), r.getImpact(),
                r.getRiskScore(), r.getRiskLevel(), r.getTreatmentStrategy(), null, r.getStatus())).toList();
    }

    private void apply(RiskAssessment r, RiskForm form) {
        Asset asset = assetRepository.findById(form.getAssetId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid asset."));
        AssetVulnerability av = null;
        if (form.getAssetVulnerabilityId() != null) {
            av = assetVulnerabilityRepository.findById(form.getAssetVulnerabilityId())
                    .orElseThrow(() -> new BusinessRuleException("Choose a valid vulnerability instance."));
            if (!av.getAsset().getId().equals(asset.getId())) {
                throw new BusinessRuleException("The selected vulnerability does not belong to the selected asset.");
            }
        }
        // Never trust a score from the browser: calculate it here.
        int score = calculateScore(form.getLikelihood(), form.getImpact());
        r.setTitle(form.getTitle().trim());
        r.setDescription(Texts.clean(form.getDescription()));
        r.setAsset(asset);
        r.setAssetVulnerability(av);
        r.setLikelihood(form.getLikelihood());
        r.setImpact(form.getImpact());
        r.setRiskScore(score);
        r.setRiskLevel(RiskLevel.fromScore(score));
        r.setTreatmentStrategy(form.getTreatmentStrategy());
        r.setOwner(form.getOwnerId() == null ? null : userRepository.findById(form.getOwnerId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid owner.")));
        r.setStatus(form.getStatus() == null ? RiskStatus.OPEN : form.getStatus());
    }
}

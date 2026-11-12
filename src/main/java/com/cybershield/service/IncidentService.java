package com.cybershield.service;

import com.cybershield.dto.AuditLogView;
import com.cybershield.dto.IncidentForm;
import com.cybershield.dto.IncidentViews;
import com.cybershield.entity.Asset;
import com.cybershield.entity.Incident;
import com.cybershield.entity.IncidentAlert;
import com.cybershield.entity.IncidentAsset;
import com.cybershield.entity.InvestigationNote;
import com.cybershield.entity.SecurityAlert;
import com.cybershield.entity.User;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.IncidentType;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.Severity;
import com.cybershield.enums.UserStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.IncidentAlertRepository;
import com.cybershield.repository.IncidentAssetRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.InvestigationNoteRepository;
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.util.Codes;
import com.cybershield.util.Texts;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Incident management: investigation workflow, linked alerts (many-to-many), affected assets (many-to-many),
 * investigation notes and a timeline built from notes plus the audit log.
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentAlertRepository incidentAlertRepository;
    private final IncidentAssetRepository incidentAssetRepository;
    private final InvestigationNoteRepository noteRepository;
    private final SecurityAlertRepository alertRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public IncidentService(IncidentRepository incidentRepository, IncidentAlertRepository incidentAlertRepository,
                           IncidentAssetRepository incidentAssetRepository, InvestigationNoteRepository noteRepository,
                           SecurityAlertRepository alertRepository, AssetRepository assetRepository,
                           UserRepository userRepository, NotificationService notificationService,
                           AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.incidentRepository = incidentRepository;
        this.incidentAlertRepository = incidentAlertRepository;
        this.incidentAssetRepository = incidentAssetRepository;
        this.noteRepository = noteRepository;
        this.alertRepository = alertRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<IncidentViews.IncidentRow> search(String q, IncidentStatus status, Severity severity, IncidentType type,
                                                  Pageable pageable) {
        return incidentRepository.search(Texts.clean(q), status, severity, type, pageable).map(IncidentService::toRow);
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public List<IncidentViews.IncidentRow> recent(int limit) {
        return incidentRepository.findRecent(PageRequest.of(0, limit)).stream().map(IncidentService::toRow).toList();
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public IncidentViews.IncidentDetail detail(Long id) {
        Incident i = findDetail(id);
        List<IncidentViews.LinkedAlert> alerts = incidentAlertRepository.findByIncidentId(id).stream()
                .map(x -> new IncidentViews.LinkedAlert(x.getAlert().getId(), x.getAlert().getAlertCode(),
                        x.getAlert().getTitle(), x.getAlert().getSeverity(), x.getAlert().getStatus(),
                        x.getAlert().getDetectedAt(),
                        x.getAlert().getAsset() == null ? null : x.getAlert().getAsset().getAssetCode()))
                .toList();
        List<IncidentViews.AffectedAsset> assets = incidentAssetRepository.findByIncidentId(id).stream()
                .map(x -> new IncidentViews.AffectedAsset(x.getAsset().getId(), x.getAsset().getAssetCode(),
                        x.getAsset().getName(), x.getAsset().getDepartment().getName(), x.getAsset().getStatus(),
                        x.getImpactDescription()))
                .toList();
        List<IncidentViews.TimelineEntry> timeline = new ArrayList<>();
        for (InvestigationNote n : noteRepository.findByIncidentId(id)) {
            timeline.add(new IncidentViews.TimelineEntry(n.getCreatedAt(), "NOTE", n.getAuthor().getFullName(), n.getNote()));
        }
        for (AuditLogView log : auditLogService.historyOf(RecordType.INCIDENT, id)) {
            if (log.action() != AuditAction.NOTE) {
                timeline.add(new IncidentViews.TimelineEntry(log.createdAt(), log.action().getLabel(), log.username(),
                        log.description()));
            }
        }
        timeline.sort(Comparator.comparing(IncidentViews.TimelineEntry::at).reversed());
        return new IncidentViews.IncidentDetail(i.getId(), i.getIncidentCode(), i.getTitle(), i.getDescription(),
                i.getIncidentType(), i.getSeverity(), i.getStatus(),
                i.getAssignedAnalyst() == null ? null : i.getAssignedAnalyst().getId(),
                ViewMapper.nameOf(i.getAssignedAnalyst()), i.getDetectedAt(), i.getResolutionDetails(), i.getResolvedAt(),
                i.getCreatedBy().getFullName(), i.getCreatedAt(), i.getUpdatedAt(), i.getStatus().allowedNextStatuses(),
                alerts, assets, timeline, canEdit(i));
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public IncidentForm formFor(Long id) {
        Incident i = findDetail(id);
        IncidentForm f = new IncidentForm();
        f.setTitle(i.getTitle());
        f.setDescription(i.getDescription());
        f.setIncidentType(i.getIncidentType());
        f.setSeverity(i.getSeverity());
        f.setAssignedAnalystId(i.getAssignedAnalyst() == null ? null : i.getAssignedAnalyst().getId());
        f.setDetectedAt(i.getDetectedAt());
        return f;
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public Long create(IncidentForm form) {
        Incident i = new Incident();
        i.setIncidentCode(Codes.temporary());
        i.setTitle(form.getTitle().trim());
        i.setDescription(form.getDescription().trim());
        i.setIncidentType(form.getIncidentType());
        i.setSeverity(form.getSeverity());
        i.setDetectedAt(form.getDetectedAt());
        i.setStatus(IncidentStatus.OPEN);
        i.setCreatedBy(currentUserService.requireEntity());
        incidentRepository.save(i);
        i.setIncidentCode(Codes.format("INC", i.getId()));
        auditLogService.log(AuditAction.CREATE, AuditModule.INCIDENTS, RecordType.INCIDENT, i.getId(),
                "Created incident " + i.getIncidentCode() + " (" + i.getSeverity().getLabel() + " "
                        + i.getIncidentType().getLabel() + ").");
        if (form.getAlertId() != null) {
            linkAlertInternal(i, form.getAlertId());
        }
        if (form.getAssignedAnalystId() != null) {
            assignInternal(i, form.getAssignedAnalystId());
        }
        return i.getId();
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void update(Long id, IncidentForm form) {
        Incident i = findDetail(id);
        requireCanEdit(i);
        if (i.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Closed incidents cannot be edited.");
        }
        i.setTitle(form.getTitle().trim());
        i.setDescription(form.getDescription().trim());
        i.setIncidentType(form.getIncidentType());
        i.setSeverity(form.getSeverity());
        i.setDetectedAt(form.getDetectedAt());
        auditLogService.log(AuditAction.UPDATE, AuditModule.INCIDENTS, RecordType.INCIDENT, id,
                "Updated incident " + i.getIncidentCode() + ".");
        Long currentAnalyst = i.getAssignedAnalyst() == null ? null : i.getAssignedAnalyst().getId();
        if (form.getAssignedAnalystId() != null && !form.getAssignedAnalystId().equals(currentAnalyst)) {
            assignInternal(i, form.getAssignedAnalystId());
        }
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void assign(Long id, Long analystId) {
        Incident i = findDetail(id);
        requireCanEdit(i);
        if (analystId == null) {
            throw new BusinessRuleException("Choose an investigator.");
        }
        assignInternal(i, analystId);
    }

    /** Validated status change. RESOLVED requires resolution details. */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void changeStatus(Long id, IncidentStatus next, String resolutionDetails) {
        Incident i = findDetail(id);
        requireCanEdit(i);
        IncidentStatus old = i.getStatus();
        if (!old.canTransitionTo(next)) {
            throw new BusinessRuleException("Invalid incident status change: " + old.getLabel() + " -> "
                    + (next == null ? "nothing" : next.getLabel()) + ".");
        }
        String resolution = Texts.clean(resolutionDetails);
        if (next == IncidentStatus.RESOLVED) {
            if (resolution == null && i.getResolutionDetails() == null) {
                throw new BusinessRuleException("Record the resolution details before resolving the incident.");
            }
            if (resolution != null) {
                i.setResolutionDetails(resolution);
            }
            i.setResolvedAt(LocalDateTime.now());
        } else if (next == IncidentStatus.INVESTIGATING && old == IncidentStatus.RESOLVED) {
            i.setResolvedAt(null);
        }
        i.setStatus(next);
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.INCIDENTS, RecordType.INCIDENT, id,
                i.getIncidentCode() + ": " + old.getLabel() + " -> " + next.getLabel() + "."
                        + (resolution != null ? " Resolution: " + Texts.truncate(resolution, 300) : ""));
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void linkAlert(Long incidentId, Long alertId) {
        Incident i = findDetail(incidentId);
        requireCanEdit(i);
        if (alertId == null) {
            throw new BusinessRuleException("Choose an alert to link.");
        }
        linkAlertInternal(i, alertId);
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void linkAsset(Long incidentId, Long assetId, String impact) {
        Incident i = findDetail(incidentId);
        requireCanEdit(i);
        if (assetId == null) {
            throw new BusinessRuleException("Choose an affected asset.");
        }
        if (incidentAssetRepository.existsByIncidentIdAndAssetId(incidentId, assetId)) {
            throw new BusinessRuleException("This asset is already linked to the incident.");
        }
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> ResourceNotFoundException.of("Asset", assetId));
        IncidentAsset link = new IncidentAsset();
        link.setIncident(i);
        link.setAsset(asset);
        link.setImpactDescription(Texts.truncate(Texts.clean(impact), 500));
        link.setLinkedBy(currentUserService.requireEntity());
        incidentAssetRepository.save(link);
        auditLogService.log(AuditAction.LINK, AuditModule.INCIDENTS, RecordType.INCIDENT, incidentId,
                "Linked affected asset " + asset.getAssetCode() + " to " + i.getIncidentCode() + ".");
    }

    /** Any analyst or manager may add investigation notes (collaboration). */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void addNote(Long incidentId, String note) {
        Incident i = findDetail(incidentId);
        String text = Texts.clean(note);
        if (text == null) {
            throw new BusinessRuleException("The note cannot be empty.");
        }
        InvestigationNote n = new InvestigationNote();
        n.setIncident(i);
        n.setAuthor(currentUserService.requireEntity());
        n.setNote(Texts.truncate(text, 4000));
        noteRepository.save(n);
        auditLogService.log(AuditAction.NOTE, AuditModule.INCIDENTS, RecordType.INCIDENT, incidentId,
                "Added an investigation note to " + i.getIncidentCode() + ".");
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public List<com.cybershield.dto.Option> linkableAlerts() {
        return alertRepository.findRecentByStatusIn(List.of(AlertStatus.NEW, AlertStatus.INVESTIGATING,
                        AlertStatus.ESCALATED, AlertStatus.RESOLVED), PageRequest.of(0, 100)).stream()
                .map(a -> new com.cybershield.dto.Option(a.getId(), a.getAlertCode() + " - " + a.getTitle()))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private void linkAlertInternal(Incident i, Long alertId) {
        SecurityAlert alert = alertRepository.findById(alertId).orElseThrow(() -> ResourceNotFoundException.of("Alert", alertId));
        if (incidentAlertRepository.existsByIncidentIdAndAlertId(i.getId(), alertId)) {
            throw new BusinessRuleException(alert.getAlertCode() + " is already linked to this incident.");
        }
        IncidentAlert link = new IncidentAlert();
        link.setIncident(i);
        link.setAlert(alert);
        link.setLinkedBy(currentUserService.requireEntity());
        incidentAlertRepository.save(link);
        if (alert.getStatus() == AlertStatus.NEW || alert.getStatus() == AlertStatus.INVESTIGATING) {
            AlertStatus old = alert.getStatus();
            alert.setStatus(AlertStatus.ESCALATED);
            auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.ALERTS, RecordType.ALERT, alertId,
                    alert.getAlertCode() + ": " + old.getLabel() + " -> Escalated (linked to " + i.getIncidentCode() + ").");
        }
        auditLogService.log(AuditAction.LINK, AuditModule.INCIDENTS, RecordType.INCIDENT, i.getId(),
                "Linked alert " + alert.getAlertCode() + " to " + i.getIncidentCode() + ".");
    }

    private void assignInternal(Incident i, Long analystId) {
        User analyst = userRepository.findDetailById(analystId)
                .orElseThrow(() -> new BusinessRuleException("Choose a valid investigator."));
        RoleCode role = analyst.getRole().getCode();
        if (role != RoleCode.SECURITY_ANALYST && role != RoleCode.SECURITY_MANAGER) {
            throw new BusinessRuleException("Incidents can only be assigned to Security Analysts or Managers.");
        }
        if (analyst.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(analyst.getFullName() + " is inactive.");
        }
        i.setAssignedAnalyst(analyst);
        if (i.getStatus() == IncidentStatus.OPEN) {
            i.setStatus(IncidentStatus.INVESTIGATING);
        }
        auditLogService.log(AuditAction.ASSIGN, AuditModule.INCIDENTS, RecordType.INCIDENT, i.getId(),
                i.getIncidentCode() + " assigned to " + analyst.getUsername() + ".");
        notificationService.notify(analyst, NotificationCategory.INCIDENT_ASSIGNED,
                "Incident " + i.getIncidentCode() + " assigned to you",
                i.getTitle() + " (" + i.getSeverity().getLabel() + ").", RecordType.INCIDENT, i.getId(), null);
    }

    /** Managers may update any incident; analysts only incidents assigned to them, created by them or unassigned. */
    private boolean canEdit(Incident i) {
        if (currentUserService.isManager()) {
            return true;
        }
        Long me = currentUserService.requireId();
        return i.getAssignedAnalyst() == null || i.getAssignedAnalyst().getId().equals(me)
                || i.getCreatedBy().getId().equals(me);
    }

    private void requireCanEdit(Incident i) {
        if (!canEdit(i)) {
            throw new AccessDeniedException("Only the assigned investigator, the creator or a Security Manager "
                    + "can update this incident.");
        }
    }

    private Incident findDetail(Long id) {
        return incidentRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Incident", id));
    }

    private static IncidentViews.IncidentRow toRow(Incident i) {
        return new IncidentViews.IncidentRow(i.getId(), i.getIncidentCode(), i.getTitle(), i.getIncidentType(),
                i.getSeverity(), i.getStatus(), ViewMapper.nameOf(i.getAssignedAnalyst()), i.getDetectedAt());
    }
}

package com.cybershield.service;

import com.cybershield.dto.AlertForm;
import com.cybershield.dto.IncidentViews;
import com.cybershield.entity.SecurityAlert;
import com.cybershield.entity.User;
import com.cybershield.enums.AlertSource;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
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
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.util.Codes;
import com.cybershield.util.Texts;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Security alerts stored in MySQL (seeded demo data plus alerts entered by analysts). */
@Service
public class AlertService {

    private final SecurityAlertRepository alertRepository;
    private final IncidentAlertRepository incidentAlertRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AlertService(SecurityAlertRepository alertRepository, IncidentAlertRepository incidentAlertRepository,
                        AssetRepository assetRepository, UserRepository userRepository,
                        NotificationService notificationService, AuditLogService auditLogService,
                        CurrentUserService currentUserService) {
        this.alertRepository = alertRepository;
        this.incidentAlertRepository = incidentAlertRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<IncidentViews.AlertRow> search(String q, AlertStatus status, Severity severity, AlertSource source,
                                              boolean mine, Pageable pageable) {
        Long analystId = mine ? currentUserService.requireId() : null;
        return alertRepository.search(Texts.clean(q), status, severity, source, analystId, pageable)
                .map(s -> new IncidentViews.AlertRow(s.getId(), s.getAlertCode(), s.getTitle(), s.getSource(),
                        s.getSeverity(), s.getAsset() == null ? null : s.getAsset().getAssetCode(), s.getDetectedAt(),
                        s.getStatus(), ViewMapper.nameOf(s.getAssignedAnalyst())));
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional(readOnly = true)
    public IncidentViews.AlertDetail detail(Long id) {
        SecurityAlert s = alertRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Alert", id));
        List<IncidentViews.IncidentRow> incidents = incidentAlertRepository.findByAlertId(id).stream()
                .map(x -> new IncidentViews.IncidentRow(x.getIncident().getId(), x.getIncident().getIncidentCode(),
                        x.getIncident().getTitle(), x.getIncident().getIncidentType(), x.getIncident().getSeverity(),
                        x.getIncident().getStatus(), null, x.getIncident().getDetectedAt()))
                .toList();
        return new IncidentViews.AlertDetail(s.getId(), s.getAlertCode(), s.getTitle(), s.getDescription(), s.getSource(),
                s.getSeverity(), s.getAsset() == null ? null : s.getAsset().getId(),
                s.getAsset() == null ? null : s.getAsset().getAssetCode(),
                s.getAsset() == null ? null : s.getAsset().getName(), s.getDetectedAt(), s.getStatus(),
                s.getAssignedAnalyst() == null ? null : s.getAssignedAnalyst().getId(),
                ViewMapper.nameOf(s.getAssignedAnalyst()), ViewMapper.nameOf(s.getCreatedBy()), s.getCreatedAt(),
                s.getUpdatedAt(), s.getStatus().allowedNextStatuses(), incidents,
                auditLogService.historyOf(RecordType.ALERT, id));
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public Long create(AlertForm form) {
        if (form.getDetectedAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BusinessRuleException("Detection time cannot be in the future.");
        }
        SecurityAlert s = new SecurityAlert();
        s.setAlertCode(Codes.temporary());
        s.setTitle(form.getTitle().trim());
        s.setDescription(Texts.clean(form.getDescription()));
        s.setSource(form.getSource());
        s.setSeverity(form.getSeverity());
        s.setAsset(form.getAssetId() == null ? null : assetRepository.findById(form.getAssetId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid asset.")));
        s.setDetectedAt(form.getDetectedAt());
        s.setStatus(AlertStatus.NEW);
        s.setCreatedBy(currentUserService.requireEntity());
        alertRepository.save(s);
        s.setAlertCode(Codes.format("ALT", s.getId()));
        auditLogService.log(AuditAction.CREATE, AuditModule.ALERTS, RecordType.ALERT, s.getId(),
                "Recorded alert " + s.getAlertCode() + " (" + s.getSeverity().getLabel() + ", " + s.getSource().getLabel() + ").");
        if (form.getAssignedAnalystId() != null) {
            assignInternal(s, form.getAssignedAnalystId());
        }
        return s.getId();
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void assign(Long alertId, Long analystId) {
        SecurityAlert s = find(alertId);
        if (s.getStatus() == AlertStatus.CLOSED) {
            throw new BusinessRuleException("Closed alerts cannot be re-assigned.");
        }
        if (analystId == null) {
            throw new BusinessRuleException("Choose an analyst.");
        }
        assignInternal(s, analystId);
    }

    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void changeStatus(Long alertId, AlertStatus next) {
        SecurityAlert s = find(alertId);
        AlertStatus old = s.getStatus();
        if (!old.canTransitionTo(next)) {
            throw new BusinessRuleException("Invalid alert status change: " + old.getLabel() + " -> "
                    + (next == null ? "nothing" : next.getLabel()) + ".");
        }
        s.setStatus(next);
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.ALERTS, RecordType.ALERT, alertId,
                s.getAlertCode() + ": " + old.getLabel() + " -> " + next.getLabel() + ".");
    }

    private void assignInternal(SecurityAlert s, Long analystId) {
        User analyst = userRepository.findDetailById(analystId)
                .orElseThrow(() -> new BusinessRuleException("Choose a valid analyst."));
        RoleCode role = analyst.getRole().getCode();
        if (role != RoleCode.SECURITY_ANALYST && role != RoleCode.SECURITY_MANAGER) {
            throw new BusinessRuleException("Alerts can only be assigned to Security Analysts or Managers.");
        }
        if (analyst.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(analyst.getFullName() + " is inactive.");
        }
        s.setAssignedAnalyst(analyst);
        if (s.getStatus() == AlertStatus.NEW) {
            s.setStatus(AlertStatus.INVESTIGATING);
        }
        auditLogService.log(AuditAction.ASSIGN, AuditModule.ALERTS, RecordType.ALERT, s.getId(),
                s.getAlertCode() + " assigned to " + analyst.getUsername() + ".");
        notificationService.notify(analyst, NotificationCategory.ALERT_ASSIGNED,
                "Alert " + s.getAlertCode() + " assigned to you", s.getTitle() + " (" + s.getSeverity().getLabel() + ").",
                RecordType.ALERT, s.getId(), null);
    }

    private SecurityAlert find(Long id) {
        return alertRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Alert", id));
    }
}

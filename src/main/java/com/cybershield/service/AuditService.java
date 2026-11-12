package com.cybershield.service;

import com.cybershield.dto.AuditPlanForm;
import com.cybershield.dto.AuditViews;
import com.cybershield.dto.ControlForm;
import com.cybershield.dto.CorrectiveActionForm;
import com.cybershield.dto.FindingForm;
import com.cybershield.entity.AuditEvidence;
import com.cybershield.entity.AuditEvidenceReview;
import com.cybershield.entity.AuditFinding;
import com.cybershield.entity.AuditPlan;
import com.cybershield.entity.ComplianceControl;
import com.cybershield.entity.ComplianceFramework;
import com.cybershield.entity.CorrectiveAction;
import com.cybershield.entity.User;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditEvidenceStatus;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.AuditPlanStatus;
import com.cybershield.enums.AuditReviewDecision;
import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.Severity;
import com.cybershield.enums.UserStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.repository.AuditEvidenceRepository;
import com.cybershield.repository.AuditEvidenceReviewRepository;
import com.cybershield.repository.AuditFindingRepository;
import com.cybershield.repository.AuditPlanRepository;
import com.cybershield.repository.ComplianceControlRepository;
import com.cybershield.repository.ComplianceFrameworkRepository;
import com.cybershield.repository.CorrectiveActionRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.security.SecurityUser;
import com.cybershield.storage.FileStorageService;
import com.cybershield.storage.StoredFile;
import com.cybershield.util.Codes;
import com.cybershield.util.Texts;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Audit and compliance management: frameworks, controls, audit plans, findings, corrective actions and
 * audit evidence. Audit evidence is completely separate from remediation evidence; verifying audit evidence
 * gives an auditor NO permission over remediation evidence.
 */
@Service
public class AuditService {

    private final ComplianceFrameworkRepository frameworkRepository;
    private final ComplianceControlRepository controlRepository;
    private final AuditPlanRepository planRepository;
    private final AuditFindingRepository findingRepository;
    private final CorrectiveActionRepository actionRepository;
    private final AuditEvidenceRepository evidenceRepository;
    private final AuditEvidenceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public AuditService(ComplianceFrameworkRepository frameworkRepository, ComplianceControlRepository controlRepository,
                        AuditPlanRepository planRepository, AuditFindingRepository findingRepository,
                        CorrectiveActionRepository actionRepository, AuditEvidenceRepository evidenceRepository,
                        AuditEvidenceReviewRepository reviewRepository, UserRepository userRepository,
                        FileStorageService fileStorageService, NotificationService notificationService,
                        AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.frameworkRepository = frameworkRepository;
        this.controlRepository = controlRepository;
        this.planRepository = planRepository;
        this.findingRepository = findingRepository;
        this.actionRepository = actionRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    // ============================================================ frameworks & controls

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public List<AuditViews.Framework> frameworks() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : frameworkRepository.countControls()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return frameworkRepository.findAllByOrderByNameAsc().stream()
                .map(f -> new AuditViews.Framework(f.getId(), f.getCode(), f.getName(), f.getVersionLabel(),
                        f.getDescription(), counts.getOrDefault(f.getId(), 0L)))
                .toList();
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<AuditViews.Control> controls(Long frameworkId, String q, Pageable pageable) {
        return controlRepository.search(frameworkId, Texts.clean(q), pageable).map(c -> new AuditViews.Control(c.getId(),
                c.getFramework().getName(), c.getControlCode(), c.getTitle(), c.getDescription(), c.getCategory()));
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public Long addControl(ControlForm form) {
        ComplianceFramework framework = frameworkRepository.findById(form.getFrameworkId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid framework."));
        String code = form.getControlCode().trim();
        if (controlRepository.existsByFrameworkIdAndControlCodeIgnoreCase(framework.getId(), code)) {
            throw new BusinessRuleException("Control " + code + " already exists in " + framework.getName() + ".");
        }
        ComplianceControl c = new ComplianceControl();
        c.setFramework(framework);
        c.setControlCode(code);
        c.setTitle(form.getTitle().trim());
        c.setDescription(Texts.clean(form.getDescription()));
        c.setCategory(Texts.clean(form.getCategory()));
        controlRepository.save(c);
        auditLogService.log(AuditAction.CREATE, AuditModule.AUDITS, RecordType.COMPLIANCE_CONTROL, c.getId(),
                "Added control " + code + " to " + framework.getCode() + ".");
        return c.getId();
    }

    // ============================================================ audit plans

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<AuditViews.PlanRow> searchPlans(String q, AuditPlanStatus status, Long frameworkId, Pageable pageable) {
        return planRepository.search(Texts.clean(q), status, frameworkId, pageable).map(p -> new AuditViews.PlanRow(
                p.getId(), p.getPlanCode(), p.getName(), p.getFramework().getName(), p.getAssignedAuditor().getFullName(),
                p.getStartDate(), p.getTargetCompletionDate(), p.getStatus()));
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public AuditViews.PlanDetail planDetail(Long id) {
        AuditPlan p = findPlan(id);
        List<AuditViews.FindingRow> findings = findingRepository.findByPlanId(id).stream()
                .map(f -> findingRow(f, p.getPlanCode())).toList();
        return new AuditViews.PlanDetail(p.getId(), p.getPlanCode(), p.getName(), p.getFramework().getId(),
                p.getFramework().getName(), p.getDescription(), p.getAssignedAuditor().getFullName(),
                p.getAssignedAuditor().getId(), p.getStartDate(), p.getTargetCompletionDate(), p.getStatus(),
                p.getCreatedBy().getFullName(), p.getCreatedAt(), nextPlanStatuses(p.getStatus()), findings,
                canManagePlan(p), auditLogService.historyOf(RecordType.AUDIT_PLAN, id));
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public AuditPlanForm planFormFor(Long id) {
        AuditPlan p = findPlan(id);
        AuditPlanForm f = new AuditPlanForm();
        f.setName(p.getName());
        f.setFrameworkId(p.getFramework().getId());
        f.setDescription(p.getDescription());
        f.setAssignedAuditorId(p.getAssignedAuditor().getId());
        f.setStartDate(p.getStartDate());
        f.setTargetCompletionDate(p.getTargetCompletionDate());
        f.setStatus(p.getStatus());
        return f;
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public Long createPlan(AuditPlanForm form) {
        AuditPlan p = new AuditPlan();
        p.setPlanCode(Codes.temporary());
        applyPlan(p, form);
        p.setStatus(AuditPlanStatus.PLANNED);
        p.setCreatedBy(currentUserService.requireEntity());
        planRepository.save(p);
        p.setPlanCode(Codes.format("AUD", p.getId()));
        auditLogService.log(AuditAction.CREATE, AuditModule.AUDITS, RecordType.AUDIT_PLAN, p.getId(),
                "Created audit plan " + p.getPlanCode() + " (" + p.getName() + ").");
        return p.getId();
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public void updatePlan(Long id, AuditPlanForm form) {
        AuditPlan p = findPlan(id);
        requireCanManagePlan(p);
        if (p.getStatus() == AuditPlanStatus.COMPLETED || p.getStatus() == AuditPlanStatus.CANCELLED) {
            throw new BusinessRuleException("A " + p.getStatus().getLabel().toLowerCase() + " audit plan cannot be edited.");
        }
        AuditPlanStatus old = p.getStatus();
        applyPlan(p, form);
        if (form.getStatus() != null && form.getStatus() != old) {
            if (!nextPlanStatuses(old).contains(form.getStatus())) {
                throw new BusinessRuleException("Invalid audit plan status change: " + old.getLabel() + " -> "
                        + form.getStatus().getLabel() + ".");
            }
            p.setStatus(form.getStatus());
        }
        auditLogService.log(AuditAction.UPDATE, AuditModule.AUDITS, RecordType.AUDIT_PLAN, id,
                "Updated audit plan " + p.getPlanCode() + (old != p.getStatus()
                        ? " (status " + old.getLabel() + " -> " + p.getStatus().getLabel() + ")" : "") + ".");
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public void changePlanStatus(Long id, AuditPlanStatus next) {
        AuditPlan p = findPlan(id);
        requireCanManagePlan(p);
        AuditPlanStatus old = p.getStatus();
        if (next == null || !nextPlanStatuses(old).contains(next)) {
            throw new BusinessRuleException("Invalid audit plan status change.");
        }
        p.setStatus(next);
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.AUDITS, RecordType.AUDIT_PLAN, id,
                p.getPlanCode() + ": " + old.getLabel() + " -> " + next.getLabel() + ".");
    }

    public static Set<AuditPlanStatus> nextPlanStatuses(AuditPlanStatus s) {
        return switch (s) {
            case PLANNED -> EnumSet.of(AuditPlanStatus.IN_PROGRESS, AuditPlanStatus.CANCELLED);
            case IN_PROGRESS -> EnumSet.of(AuditPlanStatus.COMPLETED, AuditPlanStatus.CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(AuditPlanStatus.class);
        };
    }

    // ============================================================ findings

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<AuditViews.FindingRow> searchFindings(String q, FindingStatus status, Severity severity, Long planId,
                                                      Pageable pageable) {
        return findingRepository.search(Texts.clean(q), status, severity, planId, pageable)
                .map(f -> findingRow(f, f.getAuditPlan().getPlanCode()));
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public Long recordFinding(Long planId, FindingForm form) {
        AuditPlan p = findPlan(planId);
        requireCanManagePlan(p);
        if (p.getStatus() == AuditPlanStatus.COMPLETED || p.getStatus() == AuditPlanStatus.CANCELLED) {
            throw new BusinessRuleException("Findings cannot be added to a " + p.getStatus().getLabel().toLowerCase()
                    + " audit plan.");
        }
        ComplianceControl control = null;
        if (form.getControlId() != null) {
            control = controlRepository.findById(form.getControlId())
                    .orElseThrow(() -> new BusinessRuleException("Choose a valid control."));
            if (!control.getFramework().getId().equals(p.getFramework().getId())) {
                throw new BusinessRuleException("The control must belong to the plan's framework.");
            }
        }
        if (form.getDueDate() != null && form.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The due date cannot be in the past.");
        }
        AuditFinding f = new AuditFinding();
        f.setFindingCode(Codes.temporary());
        f.setAuditPlan(p);
        f.setControl(control);
        f.setTitle(form.getTitle().trim());
        f.setDescription(form.getDescription().trim());
        f.setSeverity(form.getSeverity());
        f.setStatus(FindingStatus.OPEN);
        f.setDueDate(form.getDueDate());
        f.setCreatedBy(currentUserService.requireEntity());
        findingRepository.save(f);
        f.setFindingCode(Codes.format("FND", f.getId()));
        if (p.getStatus() == AuditPlanStatus.PLANNED) {
            p.setStatus(AuditPlanStatus.IN_PROGRESS);
        }
        auditLogService.log(AuditAction.CREATE, AuditModule.AUDITS, RecordType.AUDIT_FINDING, f.getId(),
                "Recorded finding " + f.getFindingCode() + " (" + f.getSeverity().getLabel() + ") in " + p.getPlanCode() + ".");
        List<User> recipients = new ArrayList<>(notificationService.activeUsersWithRole(RoleCode.SECURITY_MANAGER));
        recipients.add(p.getAssignedAuditor());
        recipients.removeIf(u -> u.getId().equals(currentUserService.requireId()));
        notificationService.notifyAll(recipients, NotificationCategory.AUDIT_FINDING,
                "New audit finding " + f.getFindingCode(), f.getTitle() + " (" + f.getSeverity().getLabel() + ") in "
                        + p.getPlanCode() + ".", RecordType.AUDIT_FINDING, f.getId(), null);
        return f.getId();
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional(readOnly = true)
    public AuditViews.FindingDetail findingDetail(Long id) {
        AuditFinding f = findFinding(id);
        LocalDate today = LocalDate.now();
        List<AuditViews.ActionRow> actions = actionRepository.findByFindingId(id).stream()
                .map(c -> actionRow(c, f, today)).toList();
        AuditPlan p = f.getAuditPlan();
        return new AuditViews.FindingDetail(f.getId(), f.getFindingCode(), f.getTitle(), f.getDescription(),
                f.getSeverity(), f.getStatus(), f.getDueDate(), p.getId(), p.getPlanCode(), p.getName(),
                p.getFramework().getName(), f.getControl() == null ? null : f.getControl().getControlCode(),
                f.getControl() == null ? null : f.getControl().getTitle(), f.getCreatedBy().getFullName(),
                f.getCreatedAt(), actions, canManagePlan(p), auditLogService.historyOf(RecordType.AUDIT_FINDING, id));
    }

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public void changeFindingStatus(Long id, FindingStatus next) {
        AuditFinding f = findFinding(id);
        requireCanManagePlan(f.getAuditPlan());
        if (next == null || next == f.getStatus()) {
            throw new BusinessRuleException("Choose a different status.");
        }
        if ((next == FindingStatus.RESOLVED || next == FindingStatus.CLOSED)
                && actionRepository.countUnverifiedForFinding(id) > 0) {
            throw new BusinessRuleException("All corrective actions must be verified before the finding is "
                    + next.getLabel().toLowerCase() + ".");
        }
        FindingStatus old = f.getStatus();
        f.setStatus(next);
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.AUDITS, RecordType.AUDIT_FINDING, id,
                f.getFindingCode() + ": " + old.getLabel() + " -> " + next.getLabel() + ".");
    }

    // ============================================================ corrective actions

    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public Long addCorrectiveAction(Long findingId, CorrectiveActionForm form) {
        AuditFinding f = findFinding(findingId);
        requireCanManagePlan(f.getAuditPlan());
        if (f.getStatus() == FindingStatus.CLOSED) {
            throw new BusinessRuleException("Corrective actions cannot be added to a closed finding.");
        }
        User assignee = userRepository.findDetailById(form.getAssignedToId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid user."));
        if (assignee.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(assignee.getFullName() + " is inactive.");
        }
        if (form.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The deadline cannot be in the past.");
        }
        CorrectiveAction c = new CorrectiveAction();
        c.setFinding(f);
        c.setAssignedTo(assignee);
        c.setDescription(form.getDescription().trim());
        c.setDeadline(form.getDeadline());
        c.setStatus(CorrectiveActionStatus.OPEN);
        c.setCreatedBy(currentUserService.requireEntity());
        actionRepository.save(c);
        if (f.getStatus() == FindingStatus.OPEN || f.getStatus() == FindingStatus.RESOLVED) {
            f.setStatus(FindingStatus.IN_PROGRESS);
        }
        auditLogService.log(AuditAction.ASSIGN, AuditModule.AUDITS, RecordType.CORRECTIVE_ACTION, c.getId(),
                "Corrective action for " + f.getFindingCode() + " assigned to " + assignee.getUsername()
                        + " (deadline " + c.getDeadline() + ").");
        notificationService.notify(assignee, NotificationCategory.CORRECTIVE_ACTION_ASSIGNED,
                "Corrective action for " + f.getFindingCode(),
                Texts.truncate(c.getDescription(), 300) + " Deadline: " + c.getDeadline() + ".",
                RecordType.CORRECTIVE_ACTION, c.getId(), null);
        return c.getId();
    }

    /** Auditors/managers see all corrective actions; other users only see actions assigned to them. */
    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional(readOnly = true)
    public Page<AuditViews.ActionRow> searchActions(CorrectiveActionStatus status, boolean mine, Pageable pageable) {
        boolean auditTeam = currentUserService.isAuditor() || currentUserService.isManager();
        Long assignedTo = (mine || !auditTeam) ? currentUserService.requireId() : null;
        LocalDate today = LocalDate.now();
        return actionRepository.search(status, assignedTo, pageable).map(c -> actionRow(c, c.getFinding(), today));
    }

    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional(readOnly = true)
    public AuditViews.ActionDetail actionDetail(Long id) {
        CorrectiveAction c = findAction(id);
        SecurityUser me = currentUserService.require();
        boolean assignee = c.getAssignedTo().getId().equals(me.getId());
        boolean auditTeam = me.hasRole(RoleCode.COMPLIANCE_AUDITOR) || me.hasRole(RoleCode.SECURITY_MANAGER);
        if (!assignee && !auditTeam) {
            throw new AccessDeniedException("You can only open corrective actions assigned to you.");
        }
        List<AuditEvidence> evidence = evidenceRepository.findByActionId(id);
        boolean submittedByMe = evidence.stream().anyMatch(e -> e.getStatus() == AuditEvidenceStatus.SUBMITTED
                && e.getSubmittedBy().getId().equals(me.getId()));
        AuditFinding f = c.getFinding();
        return new AuditViews.ActionDetail(c.getId(), f.getId(), f.getFindingCode(), f.getTitle(),
                f.getAuditPlan().getPlanCode(), f.getAuditPlan().getAssignedAuditor().getFullName(),
                c.getAssignedTo().getFullName(), c.getAssignedTo().getId(), c.getDescription(), c.getDeadline(),
                c.getStatus(), isOverdue(c, LocalDate.now()), c.getCreatedBy().getFullName(), c.getCreatedAt(),
                evidence.stream().map(e -> new AuditViews.EvidenceLine(e.getId(), e.getOriginalFilename(), e.getFileSize(),
                        e.getDescription(), e.getStatus(), e.getSubmittedBy().getFullName(), e.getSubmittedBy().getId(),
                        e.getSubmittedAt())).toList(),
                reviewRepository.findByActionId(id).stream().map(r -> new AuditViews.ReviewLine(
                        r.getEvidence().getOriginalFilename(), r.getReviewer().getFullName(), r.getDecision(),
                        r.getReviewNotes(), r.getReviewedAt())).toList(),
                assignee && c.getStatus() == CorrectiveActionStatus.OPEN,
                assignee && (c.getStatus() == CorrectiveActionStatus.OPEN || c.getStatus() == CorrectiveActionStatus.IN_PROGRESS),
                auditTeam && c.getStatus() == CorrectiveActionStatus.PENDING_REVIEW && !submittedByMe);
    }

    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional
    public void startAction(Long id) {
        CorrectiveAction c = findAction(id);
        requireActionAssignee(c);
        if (c.getStatus() != CorrectiveActionStatus.OPEN) {
            throw new BusinessRuleException("Only open corrective actions can be started.");
        }
        c.setStatus(CorrectiveActionStatus.IN_PROGRESS);
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.AUDITS, RecordType.CORRECTIVE_ACTION, id,
                "Corrective action for " + c.getFinding().getFindingCode() + ": Open -> In Progress.");
    }

    /** The responsible user uploads audit evidence; the action goes to PENDING_REVIEW and the auditor is notified. */
    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional
    public void uploadEvidence(Long actionId, MultipartFile file, String description) {
        CorrectiveAction c = findAction(actionId);
        requireActionAssignee(c);
        if (c.getStatus() != CorrectiveActionStatus.OPEN && c.getStatus() != CorrectiveActionStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Evidence can only be uploaded while the corrective action is Open or In Progress.");
        }
        StoredFile stored = fileStorageService.store(file, "audit");
        AuditEvidence e = new AuditEvidence();
        e.setCorrectiveAction(c);
        e.setOriginalFilename(stored.originalFilename());
        e.setStoredFilename(stored.storedFilename());
        e.setFilePath(stored.relativePath());
        e.setMimeType(stored.mimeType());
        e.setFileSize(stored.size());
        e.setDescription(Texts.truncate(Texts.clean(description), 2000));
        e.setStatus(AuditEvidenceStatus.SUBMITTED);
        e.setSubmittedBy(currentUserService.requireEntity());
        evidenceRepository.save(e);
        c.setStatus(CorrectiveActionStatus.PENDING_REVIEW);
        AuditFinding f = c.getFinding();
        auditLogService.log(AuditAction.EVIDENCE_SUBMIT, AuditModule.AUDITS, RecordType.CORRECTIVE_ACTION, actionId,
                "Submitted audit evidence " + stored.originalFilename() + " for " + f.getFindingCode() + ".");
        notificationService.notify(f.getAuditPlan().getAssignedAuditor(), NotificationCategory.AUDIT_EVIDENCE_SUBMITTED,
                "Audit evidence submitted for " + f.getFindingCode(),
                c.getAssignedTo().getFullName() + " uploaded " + stored.originalFilename() + ". Please verify it.",
                RecordType.CORRECTIVE_ACTION, actionId, null);
    }

    /** Auditor/manager verifies or rejects the submitted audit evidence. The submitter cannot review it. */
    @PreAuthorize(Roles.AUDITOR_OR_MANAGER)
    @Transactional
    public void reviewEvidence(Long actionId, AuditReviewDecision decision, String notes) {
        if (decision == null) {
            throw new BusinessRuleException("Choose Verify or Reject.");
        }
        CorrectiveAction c = findAction(actionId);
        if (c.getStatus() != CorrectiveActionStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("There is no audit evidence waiting for review.");
        }
        String cleanNotes = Texts.clean(notes);
        if (decision == AuditReviewDecision.REJECTED && cleanNotes == null) {
            throw new BusinessRuleException("Please give a reason for rejecting the evidence.");
        }
        SecurityUser me = currentUserService.require();
        List<AuditEvidence> submitted = evidenceRepository.findByActionIdAndStatus(actionId, AuditEvidenceStatus.SUBMITTED);
        User reviewer = userRepository.getReferenceById(me.getId());
        User submitter = null;
        for (AuditEvidence e : submitted) {
            if (e.getSubmittedBy().getId().equals(me.getId())) {
                throw new AccessDeniedException("You cannot verify audit evidence that you submitted.");
            }
            submitter = e.getSubmittedBy();
            e.setStatus(decision == AuditReviewDecision.VERIFIED ? AuditEvidenceStatus.VERIFIED : AuditEvidenceStatus.REJECTED);
            AuditEvidenceReview review = new AuditEvidenceReview();
            review.setEvidence(e);
            review.setReviewer(reviewer);
            review.setDecision(decision);
            review.setReviewNotes(cleanNotes);
            reviewRepository.save(review);
        }
        AuditFinding f = c.getFinding();
        if (decision == AuditReviewDecision.VERIFIED) {
            c.setStatus(CorrectiveActionStatus.VERIFIED);
            if (actionRepository.countUnverifiedForFinding(f.getId()) == 0) {
                f.setStatus(FindingStatus.RESOLVED);
            }
        } else {
            c.setStatus(CorrectiveActionStatus.IN_PROGRESS);
        }
        auditLogService.log(decision == AuditReviewDecision.VERIFIED ? AuditAction.EVIDENCE_VERIFY : AuditAction.EVIDENCE_REJECT,
                AuditModule.AUDITS, RecordType.CORRECTIVE_ACTION, actionId,
                "Audit evidence for " + f.getFindingCode() + " " + (decision == AuditReviewDecision.VERIFIED
                        ? "verified" : "rejected") + (cleanNotes != null ? ": " + Texts.truncate(cleanNotes, 300) : "."));
        User recipient = submitter != null ? submitter : c.getAssignedTo();
        notificationService.notify(recipient, NotificationCategory.AUDIT_EVIDENCE_REVIEWED,
                "Audit evidence " + (decision == AuditReviewDecision.VERIFIED ? "verified" : "rejected") + " for "
                        + f.getFindingCode(),
                decision == AuditReviewDecision.VERIFIED ? "Your corrective action has been verified."
                        : "Reason: " + cleanNotes, RecordType.CORRECTIVE_ACTION, actionId, null);
    }

    @PreAuthorize(Roles.ANY_ROLE)
    @Transactional
    public RemediationService.EvidenceDownload downloadEvidence(Long evidenceId) {
        AuditEvidence e = evidenceRepository.findWithActionById(evidenceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Audit evidence", evidenceId));
        SecurityUser me = currentUserService.require();
        boolean auditTeam = me.hasRole(RoleCode.COMPLIANCE_AUDITOR) || me.hasRole(RoleCode.SECURITY_MANAGER);
        boolean assignee = e.getCorrectiveAction().getAssignedTo().getId().equals(me.getId());
        if (!auditTeam && !assignee) {
            throw new AccessDeniedException("You cannot download this audit evidence.");
        }
        auditLogService.log(AuditAction.DOWNLOAD, AuditModule.AUDITS, RecordType.CORRECTIVE_ACTION,
                e.getCorrectiveAction().getId(), "Downloaded audit evidence " + e.getOriginalFilename() + ".");
        return new RemediationService.EvidenceDownload(fileStorageService.load(e.getFilePath()),
                e.getOriginalFilename(), e.getMimeType());
    }

    // ============================================================ helpers

    /** Managers manage every plan; auditors manage plans assigned to them. */
    private boolean canManagePlan(AuditPlan p) {
        return currentUserService.isManager()
                || (currentUserService.isAuditor() && p.getAssignedAuditor().getId().equals(currentUserService.requireId()));
    }

    private void requireCanManagePlan(AuditPlan p) {
        if (!canManagePlan(p)) {
            throw new AccessDeniedException("Only the assigned auditor or a Security Manager can change this audit plan.");
        }
    }

    private void requireActionAssignee(CorrectiveAction c) {
        if (!c.getAssignedTo().getId().equals(currentUserService.requireId())) {
            throw new AccessDeniedException("This corrective action is assigned to someone else.");
        }
    }

    private void applyPlan(AuditPlan p, AuditPlanForm form) {
        if (form.getTargetCompletionDate().isBefore(form.getStartDate())) {
            throw new BusinessRuleException("The target completion date must be on or after the start date.");
        }
        User auditor = userRepository.findDetailById(form.getAssignedAuditorId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid auditor."));
        if (auditor.getRole().getCode() != RoleCode.COMPLIANCE_AUDITOR || auditor.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Audit plans must be assigned to an active Compliance Auditor.");
        }
        p.setName(form.getName().trim());
        p.setFramework(frameworkRepository.findById(form.getFrameworkId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid framework.")));
        p.setDescription(Texts.clean(form.getDescription()));
        p.setAssignedAuditor(auditor);
        p.setStartDate(form.getStartDate());
        p.setTargetCompletionDate(form.getTargetCompletionDate());
    }

    private AuditPlan findPlan(Long id) {
        return planRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Audit plan", id));
    }

    private AuditFinding findFinding(Long id) {
        return findingRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Audit finding", id));
    }

    private CorrectiveAction findAction(Long id) {
        return actionRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Corrective action", id));
    }

    private static boolean isOverdue(CorrectiveAction c, LocalDate today) {
        return c.getDeadline().isBefore(today) && c.getStatus() != CorrectiveActionStatus.VERIFIED;
    }

    private static AuditViews.FindingRow findingRow(AuditFinding f, String planCode) {
        return new AuditViews.FindingRow(f.getId(), f.getFindingCode(), f.getTitle(), planCode,
                f.getControl() == null ? null : f.getControl().getControlCode(), f.getSeverity(), f.getStatus(),
                f.getDueDate());
    }

    private static AuditViews.ActionRow actionRow(CorrectiveAction c, AuditFinding f, LocalDate today) {
        return new AuditViews.ActionRow(c.getId(), f.getFindingCode(), f.getTitle(), c.getAssignedTo().getFullName(),
                c.getDescription(), c.getDeadline(), c.getStatus(), isOverdue(c, today));
    }
}

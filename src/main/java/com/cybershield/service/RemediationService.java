package com.cybershield.service;

import com.cybershield.dto.RemediationViews;
import com.cybershield.dto.TaskCreateForm;
import com.cybershield.entity.Asset;
import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.RemediationEvidence;
import com.cybershield.entity.RemediationEvidenceReview;
import com.cybershield.entity.RemediationHistory;
import com.cybershield.entity.RemediationTask;
import com.cybershield.entity.User;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.EvidenceStatus;
import com.cybershield.enums.HistoryAction;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.Priority;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.ReviewDecision;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.UserStatus;
import com.cybershield.enums.VulnerabilityStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.mapper.ViewMapper;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.RemediationEvidenceRepository;
import com.cybershield.repository.RemediationEvidenceReviewRepository;
import com.cybershield.repository.RemediationHistoryRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.security.SecurityUser;
import com.cybershield.storage.FileStorageService;
import com.cybershield.storage.StoredFile;
import com.cybershield.util.Codes;
import com.cybershield.util.Texts;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The remediation workflow - the core of CyberShield.
 *
 * Every action is one database transaction that:
 *  1. checks the role and ownership of the current user,
 *  2. validates the state transition with {@link TaskStatus#canTransitionTo(TaskStatus)},
 *  3. updates the task (and, after approval, ONLY the related asset_vulnerabilities row),
 *  4. appends a remediation_history row,
 *  5. writes an audit_logs row and the required notifications.
 */
@Service
public class RemediationService {

    private final RemediationTaskRepository taskRepository;
    private final RemediationHistoryRepository historyRepository;
    private final RemediationEvidenceRepository evidenceRepository;
    private final RemediationEvidenceReviewRepository reviewRepository;
    private final AssetVulnerabilityRepository assetVulnerabilityRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public RemediationService(RemediationTaskRepository taskRepository, RemediationHistoryRepository historyRepository,
                              RemediationEvidenceRepository evidenceRepository,
                              RemediationEvidenceReviewRepository reviewRepository,
                              AssetVulnerabilityRepository assetVulnerabilityRepository, UserRepository userRepository,
                              FileStorageService fileStorageService, NotificationService notificationService,
                              AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewRepository = reviewRepository;
        this.assetVulnerabilityRepository = assetVulnerabilityRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    // =====================================================================================  queries

    /** Analysts and managers see every task; engineers only see tasks assigned to them. */
    @PreAuthorize(Roles.ANALYST_ENGINEER_OR_MANAGER)
    @Transactional(readOnly = true)
    public Page<RemediationViews.Row> search(String q, TaskStatus status, Priority priority, Long engineerId,
                                             boolean overdueOnly, Pageable pageable) {
        Long engineerFilter = currentUserService.isEngineer() ? currentUserService.requireId() : engineerId;
        LocalDate today = LocalDate.now();
        return taskRepository.search(Texts.clean(q), status, priority, engineerFilter, overdueOnly ? today : null, pageable)
                .map(t -> toRow(t, today));
    }

    @PreAuthorize(Roles.IS_ENGINEER)
    @Transactional(readOnly = true)
    public List<RemediationViews.HistoryLine> myRecentHistory(int limit) {
        return historyRepository.findRecentForUser(currentUserService.requireId(), PageRequest.of(0, limit)).stream()
                .map(h -> new RemediationViews.HistoryLine(h.getCreatedAt(), h.getActionType(), h.getPreviousStatus(),
                        h.getNewStatus(), null, null, h.getPerformedBy().getFullName(),
                        h.getTask().getTaskCode() + ": " + h.getDescription()))
                .toList();
    }

    @PreAuthorize(Roles.ANALYST_ENGINEER_OR_MANAGER)
    @Transactional(readOnly = true)
    public RemediationViews.TaskPage taskPage(Long taskId) {
        RemediationTask t = findDetail(taskId);
        checkCanView(t);
        SecurityUser me = currentUserService.require();
        boolean reviewer = me.hasRole(RoleCode.SECURITY_ANALYST) || me.hasRole(RoleCode.SECURITY_MANAGER);
        boolean assignee = isAssignee(t, me.getId());
        TaskStatus s = t.getStatus();

        List<RemediationViews.HistoryLine> history = historyRepository.findByTaskId(taskId).stream()
                .map(h -> new RemediationViews.HistoryLine(h.getCreatedAt(), h.getActionType(), h.getPreviousStatus(),
                        h.getNewStatus(), ViewMapper.nameOf(h.getPreviousAssignee()), ViewMapper.nameOf(h.getNewAssignee()),
                        h.getPerformedBy().getFullName(), h.getDescription()))
                .toList();
        List<RemediationEvidence> evidence = evidenceRepository.findByTaskId(taskId);
        List<RemediationViews.EvidenceLine> evidenceLines = evidence.stream()
                .map(e -> new RemediationViews.EvidenceLine(e.getId(), e.getOriginalFilename(), e.getMimeType(),
                        e.getFileSize(), e.getDescription(), e.getStatus(), e.getSubmittedBy().getFullName(),
                        e.getSubmittedAt()))
                .toList();
        List<RemediationViews.ReviewLine> reviews = reviewRepository.findByTaskId(taskId).stream()
                .map(r -> new RemediationViews.ReviewLine(r.getEvidence().getOriginalFilename(),
                        r.getReviewer().getFullName(), r.getDecision(), r.getComments(), r.getReviewedAt()))
                .toList();
        boolean hasDraft = evidence.stream().anyMatch(e -> e.getStatus() == EvidenceStatus.DRAFT);
        boolean submittedByMe = evidence.stream().anyMatch(e -> e.getStatus() == EvidenceStatus.SUBMITTED
                && e.getSubmittedBy().getId().equals(me.getId()));

        RemediationViews.Permissions permissions = new RemediationViews.Permissions(
                reviewer && (s == TaskStatus.OPEN || s == TaskStatus.ASSIGNED || s == TaskStatus.IN_PROGRESS
                        || s == TaskStatus.REOPENED),
                assignee && (s == TaskStatus.ASSIGNED || s == TaskStatus.REOPENED),
                (assignee && s != TaskStatus.COMPLETED) || reviewer,
                assignee && s == TaskStatus.IN_PROGRESS,
                assignee && s == TaskStatus.IN_PROGRESS && hasDraft,
                reviewer && s == TaskStatus.PENDING_VERIFICATION && !submittedByMe,
                reviewer && s == TaskStatus.COMPLETED,
                reviewer || assignee);
        return new RemediationViews.TaskPage(toDetail(t), history, evidenceLines, reviews, permissions, hasDraft);
    }

    @PreAuthorize(Roles.ANALYST_ENGINEER_OR_MANAGER)
    @Transactional(readOnly = true)
    public RemediationViews.Progress progress() {
        Long engineerFilter = currentUserService.isEngineer() ? currentUserService.requireId() : null;
        return progressFor(engineerFilter);
    }

    /** Completion % = completed / total x 100, calculated from MySQL every time (0 when there are no tasks). */
    @Transactional(readOnly = true)
    public RemediationViews.Progress progressFor(Long engineerId) {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus st : TaskStatus.values()) {
            counts.put(st, 0L);
        }
        for (Object[] row : taskRepository.countByStatus(engineerId)) {
            counts.put((TaskStatus) row[0], (Long) row[1]);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long completed = counts.get(TaskStatus.COMPLETED);
        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        return new RemediationViews.Progress(total, counts.get(TaskStatus.OPEN), counts.get(TaskStatus.ASSIGNED),
                counts.get(TaskStatus.IN_PROGRESS), counts.get(TaskStatus.PENDING_VERIFICATION), completed,
                counts.get(TaskStatus.REOPENED), taskRepository.countOverdue(LocalDate.now(), engineerId), percent);
    }

    // =====================================================================================  commands

    /** Steps 1-7 of the analyst workflow: create a task for one asset vulnerability, optionally assigning it. */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public Long createTask(TaskCreateForm form) {
        AssetVulnerability av = assetVulnerabilityRepository.findDetailById(form.getAssetVulnerabilityId())
                .orElseThrow(() -> ResourceNotFoundException.of("Asset vulnerability", form.getAssetVulnerabilityId()));
        Asset asset = av.getAsset();
        if (asset.getStatus() == AssetStatus.ARCHIVED) {
            throw new BusinessRuleException("Remediation tasks cannot be created for an archived asset.");
        }
        if (av.getStatus() != VulnerabilityStatus.OPEN && av.getStatus() != VulnerabilityStatus.MITIGATING) {
            throw new BusinessRuleException("Tasks can only be created for vulnerabilities that are Open or Mitigating "
                    + "(current status: " + av.getStatus().getLabel() + ").");
        }
        if (taskRepository.countActiveForAssetVulnerability(av.getId()) > 0) {
            throw new BusinessRuleException("An active remediation task already exists for "
                    + av.getVulnerability().getVulnCode() + " on " + asset.getAssetCode() + ".");
        }
        validateDeadline(form.getDueDate());
        if (form.getEngineerId() != null && form.getDueDate() == null) {
            throw new BusinessRuleException("Set a deadline when assigning the task to an engineer.");
        }
        User me = currentUserService.requireEntity();
        RemediationTask task = new RemediationTask();
        task.setTaskCode(Codes.temporary());
        task.setAssetVulnerability(av);
        task.setTitle(form.getTitle().trim());
        task.setDescription(form.getDescription().trim());
        task.setPriority(form.getPriority());
        task.setDueDate(form.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setCreatedBy(me);
        taskRepository.save(task);
        task.setTaskCode(Codes.format("REM", task.getId()));

        addHistory(task, HistoryAction.CREATED, null, TaskStatus.OPEN, null, null,
                "Task created for " + av.getVulnerability().getVulnCode() + " on " + asset.getAssetCode() + ".");
        auditLogService.log(AuditAction.CREATE, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, task.getId(),
                "Created remediation task " + task.getTaskCode() + " for " + av.getVulnerability().getVulnCode()
                        + " on " + asset.getAssetCode() + ".");
        if (form.getEngineerId() != null) {
            assignInternal(task, form.getEngineerId(), form.getDueDate());
        }
        return task.getId();
    }

    /** Assigns or re-assigns an engineer and sets the deadline. OPEN -> ASSIGNED. */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void assign(Long taskId, Long engineerId, LocalDate dueDate) {
        RemediationTask task = findDetail(taskId);
        TaskStatus s = task.getStatus();
        if (s == TaskStatus.PENDING_VERIFICATION || s == TaskStatus.COMPLETED) {
            throw new BusinessRuleException("A task that is " + s.getLabel() + " cannot be re-assigned.");
        }
        if (engineerId == null) {
            throw new BusinessRuleException("Choose an engineer.");
        }
        if (dueDate == null) {
            throw new BusinessRuleException("Set a deadline for the task.");
        }
        validateDeadline(dueDate);
        assignInternal(task, engineerId, dueDate);
    }

    /** Engineer starts work: ASSIGNED or REOPENED -> IN_PROGRESS. Only the assigned engineer may do this. */
    @PreAuthorize(Roles.IS_ENGINEER)
    @Transactional
    public void startWork(Long taskId) {
        RemediationTask task = findDetail(taskId);
        requireAssignee(task);
        TaskStatus old = task.getStatus();
        if (old != TaskStatus.ASSIGNED && old != TaskStatus.REOPENED) {
            throw invalidTransition(old, TaskStatus.IN_PROGRESS);
        }
        changeStatus(task, TaskStatus.IN_PROGRESS);
        AssetVulnerability av = task.getAssetVulnerability();
        if (av.getStatus() == VulnerabilityStatus.OPEN) {
            av.setStatus(VulnerabilityStatus.MITIGATING);
        }
        addHistory(task, HistoryAction.STATUS_CHANGED, old, TaskStatus.IN_PROGRESS, null, null, "Work started.");
        auditLogService.log(AuditAction.STATUS_CHANGE, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                task.getTaskCode() + ": " + old.getLabel() + " -> In Progress.");
    }

    /** Adds a remediation note. The assigned engineer (until completion) and analysts/managers may add notes. */
    @PreAuthorize(Roles.ANALYST_ENGINEER_OR_MANAGER)
    @Transactional
    public void addNote(Long taskId, String note) {
        RemediationTask task = findDetail(taskId);
        String text = Texts.clean(note);
        if (text == null) {
            throw new BusinessRuleException("The note cannot be empty.");
        }
        if (text.length() > 4000) {
            throw new BusinessRuleException("Notes are limited to 4000 characters.");
        }
        if (currentUserService.isEngineer()) {
            requireAssignee(task);
            if (task.getStatus() == TaskStatus.COMPLETED) {
                throw new BusinessRuleException("Completed tasks cannot be changed by the engineer.");
            }
        }
        addHistory(task, HistoryAction.NOTE_ADDED, null, null, null, null, text);
        auditLogService.log(AuditAction.NOTE, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                "Added a note to " + task.getTaskCode() + ".");
    }

    /** Engineer uploads an evidence file (status DRAFT until submitted). Task must be IN_PROGRESS. */
    @PreAuthorize(Roles.IS_ENGINEER)
    @Transactional
    public Long uploadEvidence(Long taskId, MultipartFile file, String description) {
        RemediationTask task = findDetail(taskId);
        requireAssignee(task);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Evidence can only be uploaded while the task is In Progress.");
        }
        StoredFile stored = fileStorageService.store(file, "remediation");
        RemediationEvidence e = new RemediationEvidence();
        e.setTask(task);
        e.setOriginalFilename(stored.originalFilename());
        e.setStoredFilename(stored.storedFilename());
        e.setFilePath(stored.relativePath());
        e.setMimeType(stored.mimeType());
        e.setFileSize(stored.size());
        e.setDescription(Texts.truncate(Texts.clean(description), 2000));
        e.setStatus(EvidenceStatus.DRAFT);
        e.setSubmittedBy(currentUserService.requireEntity());
        evidenceRepository.save(e);
        addHistory(task, HistoryAction.EVIDENCE_UPLOADED, null, null, null, null,
                "Uploaded evidence file " + stored.originalFilename() + ".");
        auditLogService.log(AuditAction.EVIDENCE_UPLOAD, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                "Uploaded evidence " + stored.originalFilename() + " to " + task.getTaskCode() + ".");
        return e.getId();
    }

    /** Removes a DRAFT evidence file that has not been submitted yet (submitted evidence is never deleted). */
    @PreAuthorize(Roles.IS_ENGINEER)
    @Transactional
    public void deleteDraftEvidence(Long taskId, Long evidenceId) {
        RemediationTask task = findDetail(taskId);
        requireAssignee(task);
        RemediationEvidence e = evidenceRepository.findWithTaskById(evidenceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Evidence", evidenceId));
        if (!e.getTask().getId().equals(taskId) || e.getStatus() != EvidenceStatus.DRAFT) {
            throw new BusinessRuleException("Only evidence that has not been submitted can be removed.");
        }
        String name = e.getOriginalFilename();
        String path = e.getFilePath();
        evidenceRepository.delete(e);
        fileStorageService.delete(path);
        addHistory(task, HistoryAction.UPDATED, null, null, null, null, "Removed draft evidence file " + name + ".");
        auditLogService.log(AuditAction.EVIDENCE_DELETE, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                "Removed draft evidence " + name + " from " + task.getTaskCode() + ".");
    }

    /** Engineer submits the uploaded evidence: IN_PROGRESS -> PENDING_VERIFICATION; reviewers are notified. */
    @PreAuthorize(Roles.IS_ENGINEER)
    @Transactional
    public void submitForVerification(Long taskId, String comment) {
        RemediationTask task = findDetail(taskId);
        requireAssignee(task);
        TaskStatus old = task.getStatus();
        if (!old.canTransitionTo(TaskStatus.PENDING_VERIFICATION)) {
            throw invalidTransition(old, TaskStatus.PENDING_VERIFICATION);
        }
        List<RemediationEvidence> drafts = evidenceRepository.findByTaskIdAndStatus(taskId, EvidenceStatus.DRAFT);
        if (drafts.isEmpty()) {
            throw new BusinessRuleException("Upload at least one evidence file before submitting for verification.");
        }
        drafts.forEach(e -> e.setStatus(EvidenceStatus.SUBMITTED));
        changeStatus(task, TaskStatus.PENDING_VERIFICATION);
        String text = "Evidence submitted for verification (" + drafts.size() + " file(s))."
                + (Texts.clean(comment) != null ? " Comment: " + comment.trim() : "");
        addHistory(task, HistoryAction.EVIDENCE_SUBMITTED, old, TaskStatus.PENDING_VERIFICATION, null, null, text);
        auditLogService.log(AuditAction.EVIDENCE_SUBMIT, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                task.getTaskCode() + ": evidence submitted, status In Progress -> Pending Verification.");

        // Recipients: the analyst/manager who created the task plus all active Security Managers.
        List<User> recipients = new ArrayList<>(notificationService.activeUsersWithRole(RoleCode.SECURITY_MANAGER));
        recipients.add(task.getCreatedBy());
        String engineer = task.getAssignedEngineer().getFullName();
        notificationService.notifyAll(recipients, NotificationCategory.EVIDENCE_SUBMITTED,
                "Evidence submitted for " + task.getTaskCode(),
                engineer + " submitted evidence for \"" + task.getTitle() + "\" ("
                        + task.getAssetVulnerability().getAsset().getAssetCode() + "). Please review it.",
                RecordType.REMEDIATION_TASK, taskId, null);
    }

    /**
     * Analyst/manager reviews the submitted evidence.
     * APPROVED: task COMPLETED and the related asset vulnerability instance MITIGATED (only that one row).
     * REJECTED: task back to IN_PROGRESS; the rejected evidence and the review are preserved.
     */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void reviewEvidence(Long taskId, ReviewDecision decision, String comments) {
        if (decision == null) {
            throw new BusinessRuleException("Choose Approve or Reject.");
        }
        RemediationTask task = findDetail(taskId);
        TaskStatus old = task.getStatus();
        TaskStatus target = decision == ReviewDecision.APPROVED ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS;
        if (old != TaskStatus.PENDING_VERIFICATION || !old.canTransitionTo(target)) {
            throw invalidTransition(old, target);
        }
        String cleanComments = Texts.clean(comments);
        if (decision == ReviewDecision.REJECTED && cleanComments == null) {
            throw new BusinessRuleException("Please give a reason for rejecting the evidence.");
        }
        SecurityUser me = currentUserService.require();
        if (isAssignee(task, me.getId())) {
            throw new AccessDeniedException("You cannot review your own remediation evidence.");
        }
        List<RemediationEvidence> submitted = evidenceRepository.findByTaskIdAndStatus(taskId, EvidenceStatus.SUBMITTED);
        if (submitted.isEmpty()) {
            throw new BusinessRuleException("There is no submitted evidence to review.");
        }
        User reviewer = userRepository.getReferenceById(me.getId());
        for (RemediationEvidence e : submitted) {
            if (e.getSubmittedBy().getId().equals(me.getId())) {
                throw new AccessDeniedException("You cannot review evidence that you submitted.");
            }
            e.setStatus(decision == ReviewDecision.APPROVED ? EvidenceStatus.APPROVED : EvidenceStatus.REJECTED);
            RemediationEvidenceReview review = new RemediationEvidenceReview();
            review.setEvidence(e);
            review.setReviewer(reviewer);
            review.setDecision(decision);
            review.setComments(cleanComments);
            reviewRepository.save(review);
        }
        changeStatus(task, target);
        User engineer = task.getAssignedEngineer();
        AssetVulnerability av = task.getAssetVulnerability();
        String where = av.getVulnerability().getVulnCode() + " on " + av.getAsset().getAssetCode();
        if (decision == ReviewDecision.APPROVED) {
            task.setCompletedAt(LocalDateTime.now());
            av.setStatus(VulnerabilityStatus.MITIGATED);
            av.setLastObservedDate(LocalDate.now());
            addHistory(task, HistoryAction.EVIDENCE_APPROVED, old, target, null, null,
                    "Evidence approved. " + where + " marked Mitigated."
                            + (cleanComments != null ? " Comments: " + cleanComments : ""));
            auditLogService.log(AuditAction.EVIDENCE_APPROVE, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                    task.getTaskCode() + ": evidence approved, task Completed, " + where + " Mitigated.");
            notificationService.notify(engineer, NotificationCategory.EVIDENCE_APPROVED,
                    "Evidence approved for " + task.getTaskCode(),
                    "Your evidence was approved by " + me.getFullName() + ". The task is completed.",
                    RecordType.REMEDIATION_TASK, taskId, null);
            if (!task.getCreatedBy().getId().equals(me.getId())) {
                notificationService.notify(task.getCreatedBy(), NotificationCategory.TASK_COMPLETED,
                        "Task " + task.getTaskCode() + " completed",
                        where + " has been remediated and verified by " + me.getFullName() + ".",
                        RecordType.REMEDIATION_TASK, taskId, null);
            }
        } else {
            addHistory(task, HistoryAction.EVIDENCE_REJECTED, old, target, null, null,
                    "Evidence rejected. Reason: " + cleanComments);
            auditLogService.log(AuditAction.EVIDENCE_REJECT, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                    task.getTaskCode() + ": evidence rejected, task returned to In Progress.");
            notificationService.notify(engineer, NotificationCategory.EVIDENCE_REJECTED,
                    "Evidence rejected for " + task.getTaskCode(),
                    "Reason: " + cleanComments, RecordType.REMEDIATION_TASK, taskId, null);
        }
    }

    /** Reopens a completed task: COMPLETED -> REOPENED. The vulnerability instance goes back to OPEN. */
    @PreAuthorize(Roles.ANALYST_OR_MANAGER)
    @Transactional
    public void reopen(Long taskId, String reason) {
        RemediationTask task = findDetail(taskId);
        TaskStatus old = task.getStatus();
        if (!old.canTransitionTo(TaskStatus.REOPENED)) {
            throw invalidTransition(old, TaskStatus.REOPENED);
        }
        String cleanReason = Texts.clean(reason);
        if (cleanReason == null) {
            throw new BusinessRuleException("Please give a reason for reopening the task.");
        }
        changeStatus(task, TaskStatus.REOPENED);
        task.setCompletedAt(null);
        AssetVulnerability av = task.getAssetVulnerability();
        av.setStatus(VulnerabilityStatus.OPEN);
        addHistory(task, HistoryAction.REOPENED, old, TaskStatus.REOPENED, null, null, "Task reopened. Reason: " + cleanReason);
        auditLogService.log(AuditAction.REOPEN, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, taskId,
                task.getTaskCode() + " reopened. " + av.getVulnerability().getVulnCode() + " on "
                        + av.getAsset().getAssetCode() + " set back to Open.");
        notificationService.notify(task.getAssignedEngineer(), NotificationCategory.TASK_REOPENED,
                "Task " + task.getTaskCode() + " was reopened", "Reason: " + cleanReason,
                RecordType.REMEDIATION_TASK, taskId, null);
    }

    /** Returns the evidence file after checking that the current user may see this task. */
    @PreAuthorize(Roles.ANALYST_ENGINEER_OR_MANAGER)
    @Transactional
    public EvidenceDownload downloadEvidence(Long evidenceId) {
        RemediationEvidence e = evidenceRepository.findWithTaskById(evidenceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Evidence", evidenceId));
        checkCanView(e.getTask());
        Resource resource = fileStorageService.load(e.getFilePath());
        auditLogService.log(AuditAction.DOWNLOAD, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK,
                e.getTask().getId(), "Downloaded evidence " + e.getOriginalFilename() + " of " + e.getTask().getTaskCode() + ".");
        return new EvidenceDownload(resource, e.getOriginalFilename(), e.getMimeType());
    }

    public record EvidenceDownload(Resource resource, String filename, String mimeType) {
    }

    // =====================================================================================  helpers

    private void assignInternal(RemediationTask task, Long engineerId, LocalDate dueDate) {
        User engineer = userRepository.findDetailById(engineerId)
                .orElseThrow(() -> new BusinessRuleException("Choose a valid engineer."));
        if (engineer.getRole().getCode() != RoleCode.IT_INFRASTRUCTURE_ENGINEER) {
            throw new BusinessRuleException("Remediation tasks can only be assigned to IT Infrastructure Engineers.");
        }
        if (engineer.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(engineer.getFullName() + " is inactive and cannot receive tasks.");
        }
        User previous = task.getAssignedEngineer();
        if (previous != null && previous.getId().equals(engineerId) && dueDate.equals(task.getDueDate())) {
            throw new BusinessRuleException("The task is already assigned to " + engineer.getFullName()
                    + " with this deadline.");
        }
        TaskStatus old = task.getStatus();
        boolean firstAssignment = old == TaskStatus.OPEN;
        if (firstAssignment) {
            if (!old.canTransitionTo(TaskStatus.ASSIGNED)) {
                throw invalidTransition(old, TaskStatus.ASSIGNED);
            }
            changeStatus(task, TaskStatus.ASSIGNED);
        }
        task.setAssignedEngineer(engineer);
        task.setDueDate(dueDate);
        HistoryAction action = firstAssignment ? HistoryAction.ASSIGNED : HistoryAction.REASSIGNED;
        addHistory(task, action, old, task.getStatus(), previous, engineer,
                (firstAssignment ? "Assigned to " : "Re-assigned to ") + engineer.getFullName() + ", due " + dueDate + ".");
        auditLogService.log(AuditAction.ASSIGN, AuditModule.REMEDIATION, RecordType.REMEDIATION_TASK, task.getId(),
                task.getTaskCode() + (firstAssignment ? " assigned to " : " re-assigned to ") + engineer.getUsername()
                        + " (due " + dueDate + ")" + (firstAssignment ? ", status Open -> Assigned." : "."));
        if (previous == null || !previous.getId().equals(engineerId)) {
            notificationService.notify(engineer, NotificationCategory.TASK_ASSIGNED,
                    "New remediation task " + task.getTaskCode(),
                    task.getTitle() + " on " + task.getAssetVulnerability().getAsset().getAssetCode() + ", due "
                            + dueDate + ".", RecordType.REMEDIATION_TASK, task.getId(), null);
        }
    }

    private void changeStatus(RemediationTask task, TaskStatus next) {
        if (!task.getStatus().canTransitionTo(next)) {
            throw invalidTransition(task.getStatus(), next);
        }
        task.setStatus(next);
    }

    private BusinessRuleException invalidTransition(TaskStatus from, TaskStatus to) {
        return new BusinessRuleException("Invalid status change: " + from.getLabel() + " -> " + to.getLabel() + ".");
    }

    private void addHistory(RemediationTask task, HistoryAction action, TaskStatus previousStatus, TaskStatus newStatus,
                            User previousAssignee, User newAssignee, String description) {
        RemediationHistory h = new RemediationHistory();
        h.setTask(task);
        h.setActionType(action);
        h.setPreviousStatus(previousStatus);
        h.setNewStatus(newStatus);
        h.setPreviousAssignee(previousAssignee);
        h.setNewAssignee(newAssignee);
        h.setPerformedBy(currentUserService.requireEntity());
        h.setDescription(description);
        historyRepository.save(h);
    }

    private void validateDeadline(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The deadline cannot be in the past.");
        }
        if (dueDate != null && dueDate.isAfter(LocalDate.now().plusYears(2))) {
            throw new BusinessRuleException("The deadline must be within the next two years.");
        }
    }

    private RemediationTask findDetail(Long id) {
        return taskRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("Remediation task", id));
    }

    private static boolean isAssignee(RemediationTask task, Long userId) {
        return task.getAssignedEngineer() != null && task.getAssignedEngineer().getId().equals(userId);
    }

    private void requireAssignee(RemediationTask task) {
        if (!isAssignee(task, currentUserService.requireId())) {
            throw new AccessDeniedException("This task is assigned to someone else.");
        }
    }

    private void checkCanView(RemediationTask task) {
        if (currentUserService.isEngineer() && !isAssignee(task, currentUserService.requireId())) {
            throw new AccessDeniedException("Engineers can only open tasks assigned to them.");
        }
    }

    private static RemediationViews.Row toRow(RemediationTask t, LocalDate today) {
        AssetVulnerability av = t.getAssetVulnerability();
        boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.COMPLETED;
        return new RemediationViews.Row(t.getId(), t.getTaskCode(), t.getTitle(), av.getAsset().getId(),
                av.getAsset().getAssetCode(), av.getAsset().getName(), av.getVulnerability().getVulnCode(),
                av.getVulnerability().getSeverity(), t.getPriority(), t.getStatus(),
                ViewMapper.nameOf(t.getAssignedEngineer()), t.getDueDate(), overdue);
    }

    private static RemediationViews.Detail toDetail(RemediationTask t) {
        AssetVulnerability av = t.getAssetVulnerability();
        Asset a = av.getAsset();
        boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now())
                && t.getStatus() != TaskStatus.COMPLETED;
        return new RemediationViews.Detail(t.getId(), t.getTaskCode(), t.getTitle(), t.getDescription(), t.getPriority(),
                t.getStatus(), t.getDueDate(), overdue, t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt(),
                t.getCreatedBy().getFullName(), t.getAssignedEngineer() == null ? null : t.getAssignedEngineer().getId(),
                ViewMapper.nameOf(t.getAssignedEngineer()), av.getId(), av.getStatus(), a.getId(), a.getAssetCode(),
                a.getName(), a.getHostname(), a.getIpAddress(), a.getOperatingSystem(), a.getDepartment().getName(),
                a.getLocation().getName(), a.getCriticality(), a.getEnvironment(), av.getVulnerability().getId(),
                av.getVulnerability().getVulnCode(), av.getVulnerability().getTitle(), av.getVulnerability().getSeverity(),
                av.getVulnerability().getCvssScore(), av.getVulnerability().getRecommendedRemediation());
    }
}

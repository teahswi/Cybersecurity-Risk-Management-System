package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cybershield.dto.RemediationViews;
import com.cybershield.dto.TaskCreateForm;
import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.Notification;
import com.cybershield.entity.RemediationTask;
import com.cybershield.enums.EvidenceStatus;
import com.cybershield.enums.HistoryAction;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.Priority;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.ReviewDecision;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.NotificationRepository;
import com.cybershield.repository.RemediationEvidenceRepository;
import com.cybershield.repository.RemediationHistoryRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.service.AuditLogService;
import com.cybershield.service.DeadlineReminderService;
import com.cybershield.service.RemediationService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Tests 12-19, 20, 25 and 28: the complete remediation workflow on real MySQL tables, including the
 * SRV-104 end-to-end demonstration scenario from the project specification.
 */
class RemediationWorkflowTest extends IntegrationTestBase {

    @Autowired
    RemediationService remediationService;
    @Autowired
    RemediationTaskRepository taskRepository;
    @Autowired
    RemediationHistoryRepository historyRepository;
    @Autowired
    RemediationEvidenceRepository evidenceRepository;
    @Autowired
    AssetVulnerabilityRepository assetVulnerabilityRepository;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    AuditLogService auditLogService;
    @Autowired
    DeadlineReminderService deadlineReminderService;
    @Autowired
    TestData data;

    private Long createSrv104Task(boolean assignNow) {
        loginAs("analyst");
        AssetVulnerability av = data.instance("SRV-104", "VUL-0001");
        TaskCreateForm form = new TaskCreateForm();
        form.setAssetVulnerabilityId(av.getId());
        form.setTitle("Patch web framework on SRV-104");
        form.setDescription("Upgrade the framework module to the approved baseline and restart the service.");
        form.setPriority(Priority.CRITICAL);
        if (assignNow) {
            form.setEngineerId(data.userId("engineer"));
            form.setDueDate(LocalDate.now().plusDays(7));
        }
        return remediationService.createTask(form);
    }

    private static MockMultipartFile evidenceFile() {
        return new MockMultipartFile("file", "srv104-patch-log.txt", "text/plain",
                "Framework upgraded, service restarted, scan clean.".getBytes(StandardCharsets.UTF_8));
    }

    private List<HistoryAction> historyActions(Long taskId) {
        return historyRepository.findByTaskId(taskId).stream().map(h -> h.getActionType()).toList();
    }

    private long notificationsFor(String username, NotificationCategory category, Long taskId) {
        Long userId = data.userId(username);
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(userId) && n.getCategory() == category
                        && RecordType.REMEDIATION_TASK == n.getRecordType() && taskId.equals(n.getRecordId()))
                .count();
    }

    @Test
    void taskCreationAndAssignmentCreateHistoryAuditAndNotification() {
        Long taskId = createSrv104Task(false);
        RemediationTask task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(task.getTaskCode()).isEqualTo(String.format("REM-%04d", taskId));
        assertThat(historyActions(taskId)).containsExactly(HistoryAction.CREATED);

        remediationService.assign(taskId, data.userId("engineer"), LocalDate.now().plusDays(5));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(task.getAssignedEngineer().getUsername()).isEqualTo("engineer");
        var history = historyRepository.findByTaskId(taskId);
        assertThat(history).extracting(h -> h.getActionType()).containsExactly(HistoryAction.CREATED, HistoryAction.ASSIGNED);
        assertThat(history.get(1).getNewAssignee().getUsername()).isEqualTo("engineer");
        assertThat(history.get(1).getPreviousStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(history.get(1).getNewStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(notificationsFor("engineer", NotificationCategory.TASK_ASSIGNED, taskId)).isEqualTo(1);
        assertThat(auditLogService.historyOf(RecordType.REMEDIATION_TASK, taskId)).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void invalidTransitionsAndRulesAreRejected() {
        Long taskId = createSrv104Task(true);
        // a second active task for the same asset vulnerability is not allowed
        assertThatThrownBy(() -> createSrv104Task(false)).isInstanceOf(BusinessRuleException.class);
        // an analyst cannot review a task that is not pending verification
        assertThatThrownBy(() -> remediationService.reviewEvidence(taskId, ReviewDecision.APPROVED, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Invalid status change");
        // a deadline in the past is rejected
        assertThatThrownBy(() -> remediationService.assign(taskId, data.userId("engineer"), LocalDate.now().minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
        // tasks can only be assigned to engineers
        assertThatThrownBy(() -> remediationService.assign(taskId, data.userId("auditor"), LocalDate.now().plusDays(3)))
                .isInstanceOf(BusinessRuleException.class);

        loginAs("engineer");
        // cannot submit before starting (ASSIGNED -> PENDING_VERIFICATION is invalid)
        assertThatThrownBy(() -> remediationService.submitForVerification(taskId, null))
                .isInstanceOf(BusinessRuleException.class);
        remediationService.startWork(taskId);
        // cannot submit without evidence
        assertThatThrownBy(() -> remediationService.submitForVerification(taskId, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("evidence");
        // cannot start twice
        assertThatThrownBy(() -> remediationService.startWork(taskId)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void evidenceRejectionReturnsTaskToInProgressAndPreservesHistory() {
        Long taskId = createSrv104Task(true);
        loginAs("engineer");
        remediationService.startWork(taskId);
        Long evidenceId = remediationService.uploadEvidence(taskId, evidenceFile(), "first attempt");
        remediationService.submitForVerification(taskId, "please review");

        loginAs("analyst");
        assertThatThrownBy(() -> remediationService.reviewEvidence(taskId, ReviewDecision.REJECTED, " "))
                .as("a rejection reason is required").isInstanceOf(BusinessRuleException.class);
        remediationService.reviewEvidence(taskId, ReviewDecision.REJECTED, "Screenshot of the version check is missing");

        RemediationTask task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(evidenceRepository.findById(evidenceId).orElseThrow().getStatus())
                .as("rejected evidence is preserved").isEqualTo(EvidenceStatus.REJECTED);
        assertThat(historyActions(taskId)).contains(HistoryAction.EVIDENCE_REJECTED);
        assertThat(notificationsFor("engineer", NotificationCategory.EVIDENCE_REJECTED, taskId)).isEqualTo(1);
        assertThat(data.instance("SRV-104", "VUL-0001").getStatus()).isEqualTo(VulnerabilityStatus.MITIGATING);
    }

    /** Test 28 - the full SRV-104 demonstration: analyst -> engineer -> reviewer, with every side effect checked. */
    @Test
    void completeSrv104RemediationWorkflow() {
        RemediationViews.Progress before = remediationService.progressFor(null);

        // Analyst creates and assigns the task (steps 1-9)
        Long taskId = createSrv104Task(true);
        assertThat(taskRepository.findById(taskId).orElseThrow().getStatus()).isEqualTo(TaskStatus.ASSIGNED);

        // Engineer works on it (steps 10-17)
        loginAs("engineer");
        var myTasks = remediationService.search(null, null, null, null, false, PageRequest.of(0, 50));
        assertThat(myTasks.getContent()).extracting(RemediationViews.Row::id).contains(taskId);
        assertThat(myTasks.getContent()).allMatch(r -> "Vikram Singh".equals(r.engineerName()));
        RemediationViews.TaskPage page = remediationService.taskPage(taskId);
        assertThat(page.task().assetCode()).isEqualTo("SRV-104");
        assertThat(page.permissions().canStart()).isTrue();
        assertThat(page.permissions().canReview()).isFalse();

        remediationService.startWork(taskId);
        remediationService.addNote(taskId, "Change window approved. Upgrading the framework now.");
        remediationService.uploadEvidence(taskId, evidenceFile(), "Upgrade log");
        remediationService.submitForVerification(taskId, "Upgrade done");
        assertThat(taskRepository.findById(taskId).orElseThrow().getStatus()).isEqualTo(TaskStatus.PENDING_VERIFICATION);

        // Reviewers are notified (step 18): the analyst who created the task and the manager
        assertThat(notificationsFor("analyst", NotificationCategory.EVIDENCE_SUBMITTED, taskId)).isEqualTo(1);
        assertThat(notificationsFor("manager", NotificationCategory.EVIDENCE_SUBMITTED, taskId)).isEqualTo(1);

        // Manager approves (steps 19-22)
        loginAs("manager");
        RemediationViews.TaskPage reviewPage = remediationService.taskPage(taskId);
        assertThat(reviewPage.permissions().canReview()).isTrue();
        assertThat(reviewPage.evidence()).hasSize(1);
        var download = remediationService.downloadEvidence(reviewPage.evidence().getFirst().id());
        assertThat(download.filename()).isEqualTo("srv104-patch-log.txt");
        assertThat(download.resource().exists()).isTrue();
        remediationService.reviewEvidence(taskId, ReviewDecision.APPROVED, "Verified with re-scan");

        RemediationTask task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
        // only THIS asset vulnerability instance is mitigated - SRV-110 with the same vulnerability is unchanged
        assertThat(data.instance("SRV-104", "VUL-0001").getStatus()).isEqualTo(VulnerabilityStatus.MITIGATED);
        assertThat(data.instance("SRV-110", "VUL-0001").getStatus()).isEqualTo(VulnerabilityStatus.MITIGATING);

        // Dashboard statistics and notifications update (steps 24-25)
        RemediationViews.Progress after = remediationService.progressFor(null);
        assertThat(after.total()).isEqualTo(before.total() + 1);
        assertThat(after.completed()).isEqualTo(before.completed() + 1);
        assertThat(notificationsFor("engineer", NotificationCategory.EVIDENCE_APPROVED, taskId)).isEqualTo(1);
        assertThat(notificationsFor("analyst", NotificationCategory.TASK_COMPLETED, taskId)).isEqualTo(1);

        // Complete, ordered history and preserved evidence/verification records (steps 26-28)
        assertThat(historyActions(taskId)).containsExactly(HistoryAction.CREATED, HistoryAction.ASSIGNED,
                HistoryAction.STATUS_CHANGED, HistoryAction.NOTE_ADDED, HistoryAction.EVIDENCE_UPLOADED,
                HistoryAction.EVIDENCE_SUBMITTED, HistoryAction.EVIDENCE_APPROVED);
        RemediationViews.TaskPage finalPage = remediationService.taskPage(taskId);
        assertThat(finalPage.reviews()).hasSize(1);
        assertThat(finalPage.evidence().getFirst().status()).isEqualTo(EvidenceStatus.APPROVED);
        assertThat(auditLogService.historyOf(RecordType.REMEDIATION_TASK, taskId))
                .extracting(l -> l.action().name())
                .contains("CREATE", "ASSIGN", "STATUS_CHANGE", "NOTE", "EVIDENCE_UPLOAD", "EVIDENCE_SUBMIT",
                        "DOWNLOAD", "EVIDENCE_APPROVE");

        // Reopen goes back through the workflow
        remediationService.reopen(taskId, "Vulnerability seen again in the weekly scan");
        assertThat(taskRepository.findById(taskId).orElseThrow().getStatus()).isEqualTo(TaskStatus.REOPENED);
        assertThat(data.instance("SRV-104", "VUL-0001").getStatus()).isEqualTo(VulnerabilityStatus.OPEN);
        assertThat(notificationsFor("engineer", NotificationCategory.TASK_REOPENED, taskId)).isEqualTo(1);
    }

    @Test
    void deadlineReminderSchedulerDoesNotCreateDuplicates() {
        DeadlineReminderService.Result first = deadlineReminderService.sendReminders();
        DeadlineReminderService.Result second = deadlineReminderService.sendReminders();
        assertThat(first.overdueNotifications()).as("seeded REM-0005 is overdue").isGreaterThanOrEqualTo(1);
        assertThat(second.overdueNotifications()).isZero();
        assertThat(second.deadlineNotifications()).isZero();
        List<Notification> overdue = notificationRepository.findAll().stream()
                .filter(n -> n.getDedupKey() != null && n.getDedupKey().startsWith("TASK_OVERDUE:")).toList();
        assertThat(overdue).extracting(n -> n.getRecipient().getUsername()).contains("engineer", "manager");
    }
}

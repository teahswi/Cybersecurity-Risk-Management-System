package com.cybershield.service;

import com.cybershield.entity.RemediationTask;
import com.cybershield.entity.User;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.TaskStatus;
import com.cybershield.repository.RemediationTaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates "deadline approaching" and "overdue" notifications.
 * The deduplication key contains the task id and due date, so running the job every hour (or after a restart)
 * never creates the same reminder twice, while a new deadline produces a new reminder.
 */
@Service
public class DeadlineReminderService {

    private static final EnumSet<TaskStatus> WORK_STATUSES =
            EnumSet.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.REOPENED);

    private final RemediationTaskRepository taskRepository;
    private final NotificationService notificationService;
    private final int warningDays;

    public DeadlineReminderService(RemediationTaskRepository taskRepository, NotificationService notificationService,
                                   @Value("${cybershield.scheduler.deadline-warning-days:2}") int warningDays) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.warningDays = warningDays;
    }

    public record Result(int deadlineNotifications, int overdueNotifications) {
    }

    @Transactional
    public Result sendReminders() {
        LocalDate today = LocalDate.now();
        List<User> managers = notificationService.activeUsersWithRole(RoleCode.SECURITY_MANAGER);
        int deadline = 0;
        int overdue = 0;
        for (RemediationTask task : taskRepository.findAssignedDueOnOrBefore(today.plusDays(warningDays), WORK_STATUSES)) {
            User engineer = task.getAssignedEngineer();
            if (task.getDueDate().isBefore(today)) {
                List<User> recipients = new ArrayList<>(managers);
                recipients.add(engineer);
                overdue += notificationService.notifyAll(recipients, NotificationCategory.TASK_OVERDUE,
                        "Task " + task.getTaskCode() + " is overdue",
                        "\"" + task.getTitle() + "\" was due on " + task.getDueDate() + " and is still "
                                + task.getStatus().getLabel() + " (engineer: " + engineer.getFullName() + ").",
                        RecordType.REMEDIATION_TASK, task.getId(),
                        "TASK_OVERDUE:" + task.getId() + ":" + task.getDueDate());
            } else if (notificationService.notify(engineer, NotificationCategory.TASK_DEADLINE,
                    "Deadline approaching for " + task.getTaskCode(),
                    "\"" + task.getTitle() + "\" is due on " + task.getDueDate() + ".",
                    RecordType.REMEDIATION_TASK, task.getId(),
                    "TASK_DEADLINE:" + task.getId() + ":" + task.getDueDate())) {
                deadline++;
            }
        }
        return new Result(deadline, overdue);
    }
}

package com.cybershield.scheduler;

import com.cybershield.service.DeadlineReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the deadline / overdue reminder check periodically with Spring's @Scheduled.
 * Disabled in the test profile (tests call DeadlineReminderService directly).
 */
@Component
@ConditionalOnProperty(name = "cybershield.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DeadlineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

    private final DeadlineReminderService reminderService;

    public DeadlineReminderScheduler(DeadlineReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(initialDelayString = "${cybershield.scheduler.initial-delay-ms:20000}",
            fixedDelayString = "${cybershield.scheduler.deadline-check-interval-ms:3600000}")
    public void checkDeadlines() {
        DeadlineReminderService.Result result = reminderService.sendReminders();
        log.info("Deadline check finished: {} approaching-deadline and {} overdue notifications created.",
                result.deadlineNotifications(), result.overdueNotifications());
    }
}

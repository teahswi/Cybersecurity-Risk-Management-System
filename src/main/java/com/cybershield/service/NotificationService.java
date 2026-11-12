package com.cybershield.service;

import com.cybershield.dto.NotificationSummary;
import com.cybershield.dto.NotificationView;
import com.cybershield.entity.Notification;
import com.cybershield.entity.User;
import com.cybershield.enums.NotificationCategory;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.repository.NotificationRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.util.RecordLinks;
import com.cybershield.util.Texts;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed notifications. Every notification belongs to exactly one user, and users can only
 * read or change their own notifications.
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Creates a notification unless the recipient is inactive or a notification with the same
     * deduplication key already exists for that recipient.
     *
     * @return true when a new notification row was inserted
     */
    @Transactional
    public boolean notify(User recipient, NotificationCategory category, String title, String message,
                          RecordType recordType, Long recordId, String dedupKey) {
        if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE) {
            return false;
        }
        if (dedupKey != null && notificationRepository.existsByRecipientIdAndDedupKey(recipient.getId(), dedupKey)) {
            return false;
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setCategory(category);
        n.setTitle(Texts.truncate(title, 200));
        n.setMessage(Texts.truncate(message, 1000));
        n.setRecordType(recordType);
        n.setRecordId(recordId);
        n.setDedupKey(dedupKey);
        notificationRepository.save(n);
        return true;
    }

    /** Notifies several users once each (duplicates in the collection are ignored). */
    @Transactional
    public int notifyAll(Collection<User> recipients, NotificationCategory category, String title, String message,
                         RecordType recordType, Long recordId, String dedupKey) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User u : recipients) {
            if (u != null) {
                unique.putIfAbsent(u.getId(), u);
            }
        }
        int created = 0;
        for (User u : unique.values()) {
            if (notify(u, category, title, message, recordType, recordId, dedupKey)) {
                created++;
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<User> activeUsersWithRole(RoleCode role) {
        return userRepository.findByRoleAndStatus(role, UserStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return currentUserService.current()
                .map(u -> notificationRepository.countByRecipientIdAndReadAtIsNull(u.getId()))
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public NotificationSummary summary(int limit) {
        Long userId = currentUserService.requireId();
        List<NotificationSummary.NotificationItem> items = notificationRepository
                .findRecent(userId, PageRequest.of(0, limit)).stream()
                .map(NotificationService::toView)
                .map(v -> new NotificationSummary.NotificationItem(v.id(), v.title(), v.message(), v.categoryLabel(),
                        v.icon(), v.createdAt().format(TIME_FORMAT), v.read(), v.link()))
                .toList();
        return new NotificationSummary(notificationRepository.countByRecipientIdAndReadAtIsNull(userId), items);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> recentForCurrentUser(int limit) {
        Long userId = currentUserService.requireId();
        return notificationRepository.findRecent(userId, PageRequest.of(0, limit)).stream()
                .map(NotificationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public Page<NotificationView> pageForCurrentUser(boolean unreadOnly, Pageable pageable) {
        Long userId = currentUserService.requireId();
        return notificationRepository.findForRecipient(userId, unreadOnly, pageable).map(NotificationService::toView);
    }

    /** Marks one notification as read. Throws AccessDeniedException for someone else's notification. */
    @Transactional
    public NotificationView markRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!n.getRecipient().getId().equals(currentUserService.requireId())) {
            throw new AccessDeniedException("You can only access your own notifications.");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
        }
        return toView(n);
    }

    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUserService.requireId(), LocalDateTime.now());
    }

    public static NotificationView toView(Notification n) {
        String link = RecordLinks.urlFor(n.getRecordType(), n.getRecordId());
        return new NotificationView(n.getId(), n.getCategory(), n.getCategory().getLabel(), n.getTitle(), n.getMessage(),
                n.getCreatedAt(), n.getReadAt() != null, link == null ? "/notifications" : link, iconFor(n.getCategory()));
    }

    /** Bootstrap Icons name for each category. */
    public static String iconFor(NotificationCategory category) {
        return switch (category) {
            case CRITICAL_VULNERABILITY -> "bi-bug-fill";
            case ALERT_ASSIGNED -> "bi-bell-fill";
            case INCIDENT_ASSIGNED -> "bi-fire";
            case TASK_ASSIGNED -> "bi-clipboard-check";
            case TASK_DEADLINE -> "bi-hourglass-split";
            case TASK_OVERDUE -> "bi-exclamation-octagon-fill";
            case EVIDENCE_SUBMITTED, AUDIT_EVIDENCE_SUBMITTED -> "bi-file-earmark-arrow-up";
            case EVIDENCE_APPROVED, TASK_COMPLETED -> "bi-check-circle-fill";
            case EVIDENCE_REJECTED -> "bi-x-circle-fill";
            case TASK_REOPENED -> "bi-arrow-counterclockwise";
            case AUDIT_FINDING, CORRECTIVE_ACTION_ASSIGNED -> "bi-journal-check";
            case AUDIT_EVIDENCE_REVIEWED -> "bi-patch-check-fill";
            case SYSTEM -> "bi-info-circle";
        };
    }
}

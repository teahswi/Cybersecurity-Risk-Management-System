package com.cybershield.dto;

import com.cybershield.enums.NotificationCategory;
import java.time.LocalDateTime;

/** Notification as shown in the navbar dropdown, the notifications page and the polling REST API. */
public record NotificationView(Long id, NotificationCategory category, String categoryLabel, String title,
                               String message, LocalDateTime createdAt, boolean read, String link, String icon) {
}

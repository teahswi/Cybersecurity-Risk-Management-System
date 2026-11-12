package com.cybershield.dto;

import java.util.List;

/** JSON payload returned by GET /api/notifications/summary (polled by notifications.js). */
public record NotificationSummary(long unreadCount, List<NotificationItem> items) {

    public record NotificationItem(Long id, String title, String message, String category, String icon,
                                   String createdAt, boolean read, String link) {
    }
}

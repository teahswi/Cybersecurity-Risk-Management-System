package com.cybershield.controller.api;

import com.cybershield.dto.NotificationSummary;
import com.cybershield.service.NotificationService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Polled by notifications.js for the navbar bell. POST requests require the CSRF header. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/summary")
    public NotificationSummary summary() {
        return notificationService.summary(8);
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Map.of("ok", true, "unreadCount", notificationService.unreadCount());
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllRead() {
        int updated = notificationService.markAllRead();
        return Map.of("ok", true, "updated", updated, "unreadCount", 0);
    }
}

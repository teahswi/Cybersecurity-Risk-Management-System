package com.cybershield.controller;

import com.cybershield.dto.CurrentUserView;
import com.cybershield.service.NotificationService;
import com.cybershield.security.CurrentUserService;
import com.cybershield.util.Texts;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the logged-in user and the unread notification count to every page model (used by the layout). */
@ControllerAdvice(basePackages = "com.cybershield.controller")
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public GlobalModelAdvice(CurrentUserService currentUserService, NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public CurrentUserView currentUser() {
        return currentUserService.current()
                .map(u -> new CurrentUserView(u.getId(), u.getFullName(), u.getUsername(), u.getRoleCode(),
                        Texts.initials(u.getFullName())))
                .orElse(null);
    }

    /** Current query string without "page" (ends with '&'), used to build pagination links that keep filters. */
    @ModelAttribute("baseQuery")
    public String baseQuery(HttpServletRequest request) {
        return queryWithout(request, Set.of("page"));
    }

    /** Current query string without paging and sorting parameters, used by sortable column headers. */
    @ModelAttribute("sortBaseQuery")
    public String sortBaseQuery(HttpServletRequest request) {
        return queryWithout(request, Set.of("page", "sort", "dir"));
    }

    private static String queryWithout(HttpServletRequest request, Set<String> excluded) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            if (excluded.contains(e.getKey()) || e.getKey().startsWith("_")) {
                continue;
            }
            for (String value : e.getValue()) {
                if (value != null && !value.isEmpty()) {
                    sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append('&');
                }
            }
        }
        return sb.toString();
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount() {
        return currentUserService.current().isPresent() ? notificationService.unreadCount() : 0;
    }
}

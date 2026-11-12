package com.cybershield.controller;

import com.cybershield.dto.NotificationView;
import com.cybershield.service.NotificationService;
import com.cybershield.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "false") boolean unread, @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("page", notificationService.pageForCurrentUser(unread,
                Paging.of(page, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))));
        model.addAttribute("unread", unread);
        return "notifications/list";
    }

    /** Marks the notification as read and opens the related record. */
    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id) {
        NotificationView n = notificationService.markRead(id);
        return "redirect:" + n.link();
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id, RedirectAttributes redirect) {
        notificationService.markRead(id);
        redirect.addFlashAttribute("success", "Notification marked as read.");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(RedirectAttributes redirect) {
        int n = notificationService.markAllRead();
        redirect.addFlashAttribute("success", n + " notification(s) marked as read.");
        return "redirect:/notifications";
    }
}

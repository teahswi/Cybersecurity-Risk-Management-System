package com.cybershield.controller;

import com.cybershield.security.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Login page, home redirect and the access-denied page. Login/logout POSTs are handled by Spring Security. */
@Controller
public class AuthController {

    private final CurrentUserService currentUserService;

    public AuthController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        if (currentUserService.current().isPresent()) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("status", 403);
        model.addAttribute("message", "Your role does not have permission to open this page or perform this action.");
        return "error/403";
    }
}

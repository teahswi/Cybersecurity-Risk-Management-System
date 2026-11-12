package com.cybershield.security;

import com.cybershield.service.AuthAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/** Records a LOGOUT audit entry after the session has been invalidated. */
@Component
public class AuditLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuthAuditService authAuditService;

    public AuditLogoutSuccessHandler(AuthAuditService authAuditService) {
        this.authAuditService = authAuditService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
            authAuditService.recordLogout(user.getId(), user.getUsername(), request.getRemoteAddr());
        }
        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}

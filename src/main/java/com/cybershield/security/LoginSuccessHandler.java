package com.cybershield.security;

import com.cybershield.service.AuthAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Records a LOGIN_SUCCESS audit entry, then continues to the requested page or the dashboard. */
@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuthAuditService authAuditService;

    public LoginSuccessHandler(AuthAuditService authAuditService) {
        this.authAuditService = authAuditService;
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof SecurityUser user) {
            authAuditService.recordLoginSuccess(user.getId(), user.getUsername(), request.getRemoteAddr());
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

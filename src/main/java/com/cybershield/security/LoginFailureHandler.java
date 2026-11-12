package com.cybershield.security;

import com.cybershield.service.AuthAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** Records a LOGIN_FAILURE audit entry (never the password) and returns to the login page. */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final AuthAuditService authAuditService;

    public LoginFailureHandler(AuthAuditService authAuditService) {
        this.authAuditService = authAuditService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        boolean disabled = exception instanceof DisabledException;
        authAuditService.recordLoginFailure(username, request.getRemoteAddr(),
                disabled ? "account is inactive" : "invalid username or password");
        response.sendRedirect(request.getContextPath() + (disabled ? "/login?disabled" : "/login?error"));
    }
}

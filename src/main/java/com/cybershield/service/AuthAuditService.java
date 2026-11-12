package com.cybershield.service;

import com.cybershield.entity.User;
import com.cybershield.enums.AuditAction;
import com.cybershield.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Audit entries for login success, login failure and logout. Passwords are never recorded. */
@Service
public class AuthAuditService {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuthAuditService(AuditLogService auditLogService, UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordLoginSuccess(Long userId, String username, String ipAddress) {
        userRepository.findById(userId).ifPresent(user -> user.setLastLoginAt(LocalDateTime.now()));
        auditLogService.logAuthEvent(userId, username, AuditAction.LOGIN_SUCCESS,
                "User " + username + " logged in.", ipAddress);
    }

    @Transactional
    public void recordLoginFailure(String attemptedUsername, String ipAddress, String reason) {
        String username = attemptedUsername == null ? "" : attemptedUsername.trim();
        Optional<User> user = username.isEmpty() ? Optional.empty() : userRepository.findByUsernameIgnoreCase(username);
        auditLogService.logAuthEvent(user.map(User::getId).orElse(null),
                username.isEmpty() ? "(blank)" : username, AuditAction.LOGIN_FAILURE,
                "Failed login for '" + (username.isEmpty() ? "(blank)" : username) + "': " + reason + ".", ipAddress);
    }

    @Transactional
    public void recordLogout(Long userId, String username, String ipAddress) {
        auditLogService.logAuthEvent(userId, username, AuditAction.LOGOUT, "User " + username + " logged out.", ipAddress);
    }
}

package com.cybershield.security;

import com.cybershield.entity.User;
import com.cybershield.enums.RoleCode;
import com.cybershield.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Gives services access to the currently logged-in user. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<SecurityUser> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public SecurityUser require() {
        return current().orElseThrow(() -> new AccessDeniedException("You must be logged in."));
    }

    public Long requireId() {
        return require().getId();
    }

    /** A JPA reference (proxy) to the current user, suitable for setting foreign keys. */
    public User requireEntity() {
        return userRepository.getReferenceById(requireId());
    }

    public boolean hasRole(RoleCode code) {
        return current().map(u -> u.hasRole(code)).orElse(false);
    }

    public boolean isManager() {
        return hasRole(RoleCode.SECURITY_MANAGER);
    }

    public boolean isEngineer() {
        return hasRole(RoleCode.IT_INFRASTRUCTURE_ENGINEER);
    }

    public boolean isAnalyst() {
        return hasRole(RoleCode.SECURITY_ANALYST);
    }

    public boolean isAuditor() {
        return hasRole(RoleCode.COMPLIANCE_AUDITOR);
    }
}

package com.cybershield.service;

import com.cybershield.dto.PasswordChangeForm;
import com.cybershield.dto.ProfileForm;
import com.cybershield.dto.UserForm;
import com.cybershield.dto.UserViews;
import com.cybershield.entity.Role;
import com.cybershield.entity.User;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.exception.ResourceNotFoundException;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.RoleRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.util.Texts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management (Security Manager) and the "My Profile" functions (every user).
 * Rules enforced here: unique username/email, BCrypt passwords, users cannot change their own role or
 * deactivate themselves, and the last active Security Manager can never be removed.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    /** Simple password policy: at least 8 characters with upper case, lower case, digit and symbol. */
    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || !password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessRuleException("Passwords need 8-72 characters including upper and lower case letters, "
                    + "a digit and a symbol.");
        }
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public Page<UserViews.Row> search(String q, RoleCode role, UserStatus status, Pageable pageable) {
        return userRepository.search(Texts.clean(q), role, status, pageable).map(u -> new UserViews.Row(u.getId(),
                u.getFullName(), u.getUsername(), u.getEmail(), u.getRole().getCode(),
                u.getDepartment() == null ? null : u.getDepartment().getName(), u.getStatus(), u.getLastLoginAt()));
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public UserViews.Detail detail(Long id) {
        return toDetail(find(id), true);
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public UserForm formFor(Long id) {
        User u = find(id);
        UserForm f = new UserForm();
        f.setFullName(u.getFullName());
        f.setUsername(u.getUsername());
        f.setEmail(u.getEmail());
        f.setRole(u.getRole().getCode());
        f.setDepartmentId(u.getDepartment() == null ? null : u.getDepartment().getId());
        f.setJobTitle(u.getJobTitle());
        f.setPhone(u.getPhone());
        f.setStatus(u.getStatus());
        return f;
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public Long create(UserForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("The username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("The email " + email + " is already used by another account.");
        }
        validatePasswordStrength(form.getPassword());
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullName(form.getFullName().trim());
        u.setRole(role(form.getRole()));
        u.setDepartment(form.getDepartmentId() == null ? null : departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid department.")));
        u.setJobTitle(Texts.clean(form.getJobTitle()));
        u.setPhone(Texts.clean(form.getPhone()));
        u.setStatus(form.getStatus() == null ? UserStatus.ACTIVE : form.getStatus());
        u.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.save(u);
        auditLogService.log(AuditAction.CREATE, AuditModule.USERS, RecordType.USER, u.getId(),
                "Created user " + username + " with role " + form.getRole().getLabel() + ".");
        return u.getId();
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void update(Long id, UserForm form) {
        User u = find(id);
        Long me = currentUserService.requireId();
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("The email " + email + " is already used by another account.");
        }
        if (!u.getUsername().equalsIgnoreCase(form.getUsername().trim())) {
            throw new BusinessRuleException("Usernames cannot be changed.");
        }
        RoleCode oldRole = u.getRole().getCode();
        UserStatus oldStatus = u.getStatus();
        if (id.equals(me) && form.getRole() != oldRole) {
            throw new BusinessRuleException("You cannot change your own role.");
        }
        if (id.equals(me) && form.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }
        boolean losesManager = oldRole == RoleCode.SECURITY_MANAGER && oldStatus == UserStatus.ACTIVE
                && (form.getRole() != RoleCode.SECURITY_MANAGER || form.getStatus() == UserStatus.INACTIVE);
        if (losesManager) {
            requireAnotherActiveManager();
        }
        u.setFullName(form.getFullName().trim());
        u.setEmail(email);
        u.setRole(role(form.getRole()));
        u.setDepartment(form.getDepartmentId() == null ? null : departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new BusinessRuleException("Choose a valid department.")));
        u.setJobTitle(Texts.clean(form.getJobTitle()));
        u.setPhone(Texts.clean(form.getPhone()));
        u.setStatus(form.getStatus());
        String newPassword = Texts.clean(form.getPassword());
        if (newPassword != null) {
            validatePasswordStrength(newPassword);
            u.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        StringBuilder text = new StringBuilder("Updated user ").append(u.getUsername());
        if (oldRole != form.getRole()) {
            text.append(", role ").append(oldRole.getLabel()).append(" -> ").append(form.getRole().getLabel());
        }
        if (oldStatus != form.getStatus()) {
            text.append(", status ").append(oldStatus.getLabel()).append(" -> ").append(form.getStatus().getLabel());
        }
        if (newPassword != null) {
            text.append(", password reset");
        }
        auditLogService.log(AuditAction.UPDATE, AuditModule.USERS, RecordType.USER, id, text.append('.').toString());
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional
    public void setActive(Long id, boolean active) {
        User u = find(id);
        if (id.equals(currentUserService.requireId())) {
            throw new BusinessRuleException("You cannot change the status of your own account.");
        }
        UserStatus target = active ? UserStatus.ACTIVE : UserStatus.INACTIVE;
        if (u.getStatus() == target) {
            throw new BusinessRuleException(u.getUsername() + " is already " + target.getLabel().toLowerCase() + ".");
        }
        if (!active && u.getRole().getCode() == RoleCode.SECURITY_MANAGER) {
            requireAnotherActiveManager();
        }
        u.setStatus(target);
        auditLogService.log(active ? AuditAction.ACTIVATE : AuditAction.DEACTIVATE, AuditModule.USERS, RecordType.USER, id,
                (active ? "Activated" : "Deactivated") + " user " + u.getUsername() + ".");
    }

    // ------------------------------------------------------------ my profile

    @Transactional(readOnly = true)
    public UserViews.Detail myProfile() {
        return toDetail(find(currentUserService.requireId()), false);
    }

    @Transactional(readOnly = true)
    public ProfileForm myProfileForm() {
        User u = find(currentUserService.requireId());
        ProfileForm f = new ProfileForm();
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setJobTitle(u.getJobTitle());
        f.setPhone(u.getPhone());
        return f;
    }

    @Transactional
    public void updateMyProfile(ProfileForm form) {
        User u = find(currentUserService.requireId());
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, u.getId())) {
            throw new BusinessRuleException("The email " + email + " is already used by another account.");
        }
        u.setFullName(form.getFullName().trim());
        u.setEmail(email);
        u.setJobTitle(Texts.clean(form.getJobTitle()));
        u.setPhone(Texts.clean(form.getPhone()));
        auditLogService.log(AuditAction.PROFILE_UPDATE, AuditModule.PROFILE, RecordType.USER, u.getId(),
                "User " + u.getUsername() + " updated their profile.");
    }

    @Transactional
    public void changeMyPassword(PasswordChangeForm form) {
        User u = find(currentUserService.requireId());
        if (!passwordEncoder.matches(form.getCurrentPassword(), u.getPasswordHash())) {
            throw new BusinessRuleException("Your current password is not correct.");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("The new password and its confirmation do not match.");
        }
        if (passwordEncoder.matches(form.getNewPassword(), u.getPasswordHash())) {
            throw new BusinessRuleException("The new password must be different from the current one.");
        }
        validatePasswordStrength(form.getNewPassword());
        u.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        auditLogService.log(AuditAction.PASSWORD_CHANGE, AuditModule.PROFILE, RecordType.USER, u.getId(),
                "User " + u.getUsername() + " changed their password.");
    }

    // ------------------------------------------------------------ helpers

    private void requireAnotherActiveManager() {
        if (userRepository.countByRoleAndStatus(RoleCode.SECURITY_MANAGER, UserStatus.ACTIVE) <= 1) {
            throw new BusinessRuleException("This is the last active Security Manager. "
                    + "Create or activate another Security Manager first.");
        }
    }

    private Role role(RoleCode code) {
        return roleRepository.findByCode(code).orElseThrow(() -> new BusinessRuleException("Unknown role " + code));
    }

    private User find(Long id) {
        return userRepository.findDetailById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private UserViews.Detail toDetail(User u, boolean withActivity) {
        return new UserViews.Detail(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getRole().getCode(),
                u.getRole().getName(), u.getRole().getDescription(),
                u.getDepartment() == null ? null : u.getDepartment().getName(), u.getJobTitle(), u.getPhone(),
                u.getStatus(), u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt(),
                withActivity ? auditLogService.recentByUser(u.getId(), 25) : java.util.List.of());
    }
}

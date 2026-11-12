package com.cybershield.dto;

import com.cybershield.enums.RoleCode;

/** Information about the logged-in user that every page layout needs (sidebar, navbar, role-aware links). */
public class CurrentUserView {

    private final Long id;
    private final String fullName;
    private final String username;
    private final RoleCode roleCode;
    private final String initials;

    public CurrentUserView(Long id, String fullName, String username, RoleCode roleCode, String initials) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.roleCode = roleCode;
        this.initials = initials;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public String getRoleLabel() {
        return roleCode.getLabel();
    }

    public String getInitials() {
        return initials;
    }

    public boolean isAnalyst() {
        return roleCode == RoleCode.SECURITY_ANALYST;
    }

    public boolean isEngineer() {
        return roleCode == RoleCode.IT_INFRASTRUCTURE_ENGINEER;
    }

    public boolean isAuditor() {
        return roleCode == RoleCode.COMPLIANCE_AUDITOR;
    }

    public boolean isManager() {
        return roleCode == RoleCode.SECURITY_MANAGER;
    }

    /** Analysts and managers work on security operations (alerts, incidents, vulnerabilities). */
    public boolean isSecurityOps() {
        return isAnalyst() || isManager();
    }

    public boolean isAuditTeam() {
        return isAuditor() || isManager();
    }
}

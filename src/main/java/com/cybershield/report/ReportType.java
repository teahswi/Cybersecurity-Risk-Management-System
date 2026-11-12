package com.cybershield.report;

import com.cybershield.enums.LabeledEnum;
import com.cybershield.enums.RoleCode;
import java.util.EnumSet;
import java.util.Set;

/** Available reports and the roles allowed to generate each of them. */
public enum ReportType implements LabeledEnum {
    ASSETS("Asset inventory report", EnumSet.of(RoleCode.SECURITY_ANALYST, RoleCode.COMPLIANCE_AUDITOR, RoleCode.SECURITY_MANAGER)),
    VULNERABILITIES("Vulnerability report", EnumSet.of(RoleCode.SECURITY_ANALYST, RoleCode.SECURITY_MANAGER)),
    INCIDENTS("Incident report", EnumSet.of(RoleCode.SECURITY_ANALYST, RoleCode.SECURITY_MANAGER)),
    REMEDIATION("Remediation report", EnumSet.of(RoleCode.SECURITY_ANALYST, RoleCode.IT_INFRASTRUCTURE_ENGINEER,
            RoleCode.SECURITY_MANAGER)),
    AUDIT("Audit findings report", EnumSet.of(RoleCode.COMPLIANCE_AUDITOR, RoleCode.SECURITY_MANAGER)),
    RISKS("Risk register report", EnumSet.of(RoleCode.SECURITY_ANALYST, RoleCode.COMPLIANCE_AUDITOR, RoleCode.SECURITY_MANAGER));

    private final String title;
    private final Set<RoleCode> allowedRoles;

    ReportType(String title, Set<RoleCode> allowedRoles) {
        this.title = title;
        this.allowedRoles = allowedRoles;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAllowedFor(RoleCode role) {
        return allowedRoles.contains(role);
    }

    public String getSlug() {
        return name().toLowerCase();
    }
}

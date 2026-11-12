package com.cybershield.security;

import com.cybershield.enums.RoleCode;

/**
 * Single place for role names and @PreAuthorize expressions, so the database codes,
 * controllers and security configuration always use the same names.
 * Database code SECURITY_MANAGER  ->  authority ROLE_SECURITY_MANAGER  ->  hasRole('SECURITY_MANAGER').
 */
public final class Roles {

    public static final String PREFIX = "ROLE_";

    public static final String ANALYST = "SECURITY_ANALYST";
    public static final String ENGINEER = "IT_INFRASTRUCTURE_ENGINEER";
    public static final String AUDITOR = "COMPLIANCE_AUDITOR";
    public static final String MANAGER = "SECURITY_MANAGER";

    public static final String IS_MANAGER = "hasRole('SECURITY_MANAGER')";
    public static final String IS_ENGINEER = "hasRole('IT_INFRASTRUCTURE_ENGINEER')";
    public static final String ANALYST_OR_MANAGER = "hasAnyRole('SECURITY_ANALYST','SECURITY_MANAGER')";
    public static final String AUDITOR_OR_MANAGER = "hasAnyRole('COMPLIANCE_AUDITOR','SECURITY_MANAGER')";
    public static final String ANALYST_AUDITOR_OR_MANAGER =
            "hasAnyRole('SECURITY_ANALYST','COMPLIANCE_AUDITOR','SECURITY_MANAGER')";
    public static final String ANALYST_ENGINEER_OR_MANAGER =
            "hasAnyRole('SECURITY_ANALYST','IT_INFRASTRUCTURE_ENGINEER','SECURITY_MANAGER')";
    public static final String ANY_ROLE =
            "hasAnyRole('SECURITY_ANALYST','IT_INFRASTRUCTURE_ENGINEER','COMPLIANCE_AUDITOR','SECURITY_MANAGER')";

    private Roles() {
    }

    public static String authorityOf(RoleCode code) {
        return PREFIX + code.name();
    }
}

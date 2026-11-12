package com.cybershield.security;

import com.cybershield.enums.RoleCode;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in principal stored in the HTTP session.
 * Holds only what is needed for authorization - never the full JPA entity.
 */
public class SecurityUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final RoleCode roleCode;
    private final boolean active;

    public SecurityUser(Long id, String username, String passwordHash, String fullName, RoleCode roleCode, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleCode = roleCode;
        this.active = active;
    }

    /** Maps the canonical role code to the Spring Security authority, e.g. ROLE_SECURITY_MANAGER. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(Roles.authorityOf(roleCode)));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** INACTIVE accounts are rejected by Spring Security with a DisabledException. */
    @Override
    public boolean isEnabled() {
        return active;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public boolean hasRole(RoleCode code) {
        return roleCode == code;
    }
}

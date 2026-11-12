package com.cybershield.dto;

import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

/** User views. Password hashes are never part of any view. */
public final class UserViews {

    private UserViews() {
    }

    public record Row(Long id, String fullName, String username, String email, RoleCode role, String departmentName,
                      UserStatus status, LocalDateTime lastLoginAt) {
    }

    public record Detail(Long id, String fullName, String username, String email, RoleCode role, String roleName,
                         String roleDescription, String departmentName, String jobTitle, String phone, UserStatus status,
                         LocalDateTime lastLoginAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                         List<AuditLogView> activity) {
    }

    public record DepartmentRow(Long id, String code, String name, String description, long assets) {
    }

    public record LocationRow(Long id, String code, String name, String city, String country, String address,
                              long assets) {
    }
}

package com.cybershield.dto;

import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / edit user form (Security Manager only). The password is required when creating
 * and optional when editing (leave empty to keep the current password).
 */
public class UserForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Use letters, digits, dots, dashes or underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotNull(message = "Choose a role")
    private RoleCode role;

    private Long departmentId;

    @Size(max = 100)
    private String jobTitle;

    @Size(max = 30)
    @Pattern(regexp = "^$|^[0-9+()\\- ]{6,30}$", message = "Enter a valid phone number")
    private String phone;

    @NotNull
    private UserStatus status = UserStatus.ACTIVE;

    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public RoleCode getRole() { return role; }
    public void setRole(RoleCode role) { this.role = role; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

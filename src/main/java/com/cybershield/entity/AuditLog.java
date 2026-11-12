package com.cybershield.entity;

import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/**
 * Append-only application audit log. user is NULL for failed logins with an unknown username.
 * Table: audit_logs
 */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "username", nullable = true, length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40, columnDefinition = "VARCHAR(40)")
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 40, columnDefinition = "VARCHAR(40)")
    private AuditModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = true, length = 40, columnDefinition = "VARCHAR(40)")
    private RecordType recordType;

    @Column(name = "record_id", nullable = true)
    private Long recordId;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "ip_address", nullable = true, length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public AuditModule getModule() {
        return module;
    }

    public void setModule(AuditModule module) {
        this.module = module;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

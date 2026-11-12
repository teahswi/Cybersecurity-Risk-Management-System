package com.cybershield.dto;

import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import java.time.LocalDateTime;

public record AuditLogView(Long id, LocalDateTime createdAt, String username, AuditAction action, AuditModule module,
                           RecordType recordType, Long recordId, String description, String ipAddress, String link) {
}

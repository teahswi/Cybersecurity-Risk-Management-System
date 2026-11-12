package com.cybershield.service;

import com.cybershield.dto.SystemViews;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.UserStatus;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AuditLogRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.Roles;
import com.cybershield.storage.FileStorageService;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** System overview. The database status comes from a real "SELECT 1" query - it is never hard-coded. */
@Service
public class SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final IncidentRepository incidentRepository;
    private final RemediationTaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;

    public SystemService(JdbcTemplate jdbcTemplate, UserRepository userRepository, AssetRepository assetRepository,
                         IncidentRepository incidentRepository, RemediationTaskRepository taskRepository,
                         AuditLogRepository auditLogRepository, FileStorageService fileStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.incidentRepository = incidentRepository;
        this.taskRepository = taskRepository;
        this.auditLogRepository = auditLogRepository;
        this.fileStorageService = fileStorageService;
    }

    public boolean isDatabaseUp() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (RuntimeException e) {
            log.warn("Database health check failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public SystemViews.Health health() {
        boolean up = isDatabaseUp();
        String dbVersion = up ? safeQuery("SELECT VERSION()") : "unavailable";
        String schemaVersion = up ? safeQuery("SELECT version FROM flyway_schema_history WHERE success = 1 "
                + "ORDER BY installed_rank DESC LIMIT 1") : "unavailable";
        long users = up ? userRepository.count() : 0;
        long active = up ? userRepository.countByStatus(UserStatus.ACTIVE) : 0;
        long assets = up ? assetRepository.countByStatusNot(AssetStatus.ARCHIVED) : 0;
        long incidents = up ? incidentRepository.countByStatusIn(EnumSet.of(IncidentStatus.OPEN,
                IncidentStatus.INVESTIGATING, IncidentStatus.CONTAINED)) : 0;
        long pending = 0;
        if (up) {
            for (Object[] row : taskRepository.countByStatus(null)) {
                if (row[0] != TaskStatus.COMPLETED) {
                    pending += (Long) row[1];
                }
            }
        }
        long logs = up ? auditLogRepository.count() : 0;
        Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        String uptimeText = uptime.toDaysPart() + "d " + uptime.toHoursPart() + "h " + uptime.toMinutesPart() + "m";
        return new SystemViews.Health(up, dbVersion, schemaVersion, users, active, assets, incidents, pending, logs,
                fileStorageService.isWritable(), System.getProperty("java.version"), uptimeText);
    }

    private String safeQuery(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (RuntimeException e) {
            return "unavailable";
        }
    }
}

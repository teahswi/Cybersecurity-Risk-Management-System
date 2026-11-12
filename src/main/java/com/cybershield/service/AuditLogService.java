package com.cybershield.service;

import com.cybershield.dto.AuditLogView;
import com.cybershield.entity.AuditLog;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import com.cybershield.repository.AuditLogRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.Roles;
import com.cybershield.security.SecurityUser;
import com.cybershield.util.RecordLinks;
import com.cybershield.util.Texts;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes the append-only audit log. Called inside the same transaction as the business change,
 * so a change and its audit entry are committed (or rolled back) together.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository,
                           CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void log(AuditAction action, AuditModule module, RecordType recordType, Long recordId, String description) {
        AuditLog log = new AuditLog();
        Optional<SecurityUser> actor = currentUserService.current();
        actor.ifPresent(u -> {
            log.setUser(userRepository.getReferenceById(u.getId()));
            log.setUsername(u.getUsername());
        });
        log.setAction(action);
        log.setModule(module);
        log.setRecordType(recordType);
        log.setRecordId(recordId);
        log.setDescription(Texts.truncate(description, 1000));
        log.setIpAddress(currentIpAddress());
        auditLogRepository.save(log);
    }

    /** Used for authentication events, where the actor is given explicitly (it may not be in the security context). */
    @Transactional
    public void logAuthEvent(Long userId, String username, AuditAction action, String description, String ipAddress) {
        AuditLog log = new AuditLog();
        if (userId != null) {
            log.setUser(userRepository.getReferenceById(userId));
        }
        log.setUsername(Texts.truncate(username, 100));
        log.setAction(action);
        log.setModule(AuditModule.AUTH);
        log.setRecordType(userId != null ? RecordType.USER : null);
        log.setRecordId(userId);
        log.setDescription(Texts.truncate(description, 1000));
        log.setIpAddress(Texts.truncate(ipAddress, 45));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> historyOf(RecordType type, Long id) {
        return auditLogRepository.findByRecord(type, id).stream().map(AuditLogService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> recent(int limit) {
        return auditLogRepository.findRecent(PageRequest.of(0, limit)).stream().map(AuditLogService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> recentByUser(Long userId, int limit) {
        return auditLogRepository.findRecentByUser(userId, PageRequest.of(0, limit)).stream()
                .map(AuditLogService::toView).toList();
    }

    /** Full audit log browser - Security Manager only. */
    @PreAuthorize(Roles.IS_MANAGER)
    @Transactional(readOnly = true)
    public Page<AuditLogView> search(String q, AuditAction action, AuditModule module, Long userId,
                                     LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        return auditLogRepository.search(Texts.clean(q), action, module, userId, fromTime, toTime, pageable)
                .map(AuditLogService::toView);
    }

    public static AuditLogView toView(AuditLog log) {
        return new AuditLogView(log.getId(), log.getCreatedAt(), log.getUsername(), log.getAction(), log.getModule(),
                log.getRecordType(), log.getRecordId(), log.getDescription(), log.getIpAddress(),
                RecordLinks.urlFor(log.getRecordType(), log.getRecordId()));
    }

    private String currentIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            return Texts.truncate(request.getRemoteAddr(), 45);
        }
        return null;
    }
}

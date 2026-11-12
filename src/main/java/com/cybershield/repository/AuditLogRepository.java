package com.cybershield.repository;

import com.cybershield.entity.AuditLog;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.enums.RecordType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Append-only repository for audit_logs. There is deliberately no update or delete method,
 * so no user - not even a Security Manager - can change the log through the application.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(l.description) like lower(concat('%', :q, '%')) "
            + "or lower(l.username) like lower(concat('%', :q, '%'))) "
            + "and (:action is null or l.action = :action) and (:module is null or l.module = :module) "
            + "and (:userId is null or l.user.id = :userId) "
            + "and (:from is null or l.createdAt >= :from) and (:to is null or l.createdAt < :to)";

    AuditLog save(AuditLog log);

    long count();

    @Query(value = "select l from AuditLog l " + SEARCH_WHERE,
            countQuery = "select count(l) from AuditLog l " + SEARCH_WHERE)
    Page<AuditLog> search(@Param("q") String q, @Param("action") AuditAction action, @Param("module") AuditModule module,
                          @Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                          Pageable pageable);

    @Query("select l from AuditLog l where l.recordType = :type and l.recordId = :id order by l.createdAt asc, l.id asc")
    List<AuditLog> findByRecord(@Param("type") RecordType type, @Param("id") Long id);

    @Query("select l from AuditLog l order by l.createdAt desc, l.id desc")
    List<AuditLog> findRecent(Pageable pageable);

    @Query("select l from AuditLog l where l.user.id = :userId order by l.createdAt desc, l.id desc")
    List<AuditLog> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("select count(l) from AuditLog l where l.action = :action and l.createdAt >= :since")
    long countActionSince(@Param("action") AuditAction action, @Param("since") LocalDateTime since);
}

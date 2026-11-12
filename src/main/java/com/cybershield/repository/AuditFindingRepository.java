package com.cybershield.repository;

import com.cybershield.entity.AuditFinding;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.Severity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(f.findingCode) like lower(concat('%', :q, '%')) "
            + "or lower(f.title) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or f.status = :status) and (:severity is null or f.severity = :severity) "
            + "and (:planId is null or f.auditPlan.id = :planId)";

    @Query(value = "select f from AuditFinding f join fetch f.auditPlan left join fetch f.control " + SEARCH_WHERE,
            countQuery = "select count(f) from AuditFinding f " + SEARCH_WHERE)
    Page<AuditFinding> search(@Param("q") String q, @Param("status") FindingStatus status,
                              @Param("severity") Severity severity, @Param("planId") Long planId, Pageable pageable);

    @Query("select f from AuditFinding f join fetch f.auditPlan p join fetch p.framework join fetch p.assignedAuditor "
            + "left join fetch f.control join fetch f.createdBy where f.id = :id")
    Optional<AuditFinding> findDetailById(@Param("id") Long id);

    @Query("select f from AuditFinding f left join fetch f.control where f.auditPlan.id = :planId order by f.findingCode")
    List<AuditFinding> findByPlanId(@Param("planId") Long planId);

    long countByStatusIn(Collection<FindingStatus> statuses);

    @Query("select f from AuditFinding f join fetch f.auditPlan p left join fetch f.control "
            + "where (:planId is null or p.id = :planId) order by p.planCode, f.findingCode")
    List<AuditFinding> findForReport(@Param("planId") Long planId);
}

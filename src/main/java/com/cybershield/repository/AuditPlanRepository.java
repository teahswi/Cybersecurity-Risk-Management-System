package com.cybershield.repository;

import com.cybershield.entity.AuditPlan;
import com.cybershield.enums.AuditPlanStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditPlanRepository extends JpaRepository<AuditPlan, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(p.planCode) like lower(concat('%', :q, '%')) "
            + "or lower(p.name) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or p.status = :status) and (:frameworkId is null or p.framework.id = :frameworkId)";

    @Query(value = "select p from AuditPlan p join fetch p.framework join fetch p.assignedAuditor " + SEARCH_WHERE,
            countQuery = "select count(p) from AuditPlan p " + SEARCH_WHERE)
    Page<AuditPlan> search(@Param("q") String q, @Param("status") AuditPlanStatus status,
                           @Param("frameworkId") Long frameworkId, Pageable pageable);

    @Query("select p from AuditPlan p join fetch p.framework join fetch p.assignedAuditor join fetch p.createdBy where p.id = :id")
    Optional<AuditPlan> findDetailById(@Param("id") Long id);

    long countByStatusIn(Collection<AuditPlanStatus> statuses);

    @Query("select p from AuditPlan p join fetch p.framework where p.status in :statuses order by p.targetCompletionDate")
    List<AuditPlan> findByStatusIn(@Param("statuses") Collection<AuditPlanStatus> statuses);

    @Query("select p from AuditPlan p join fetch p.framework join fetch p.assignedAuditor order by p.planCode")
    List<AuditPlan> findAllForSelection();
}

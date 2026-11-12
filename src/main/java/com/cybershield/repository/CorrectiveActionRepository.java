package com.cybershield.repository;

import com.cybershield.entity.CorrectiveAction;
import com.cybershield.enums.CorrectiveActionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {

    @Query(value = "select c from CorrectiveAction c join fetch c.finding f join fetch c.assignedTo "
            + "where (:status is null or c.status = :status) and (:assignedToId is null or c.assignedTo.id = :assignedToId)",
            countQuery = "select count(c) from CorrectiveAction c "
            + "where (:status is null or c.status = :status) and (:assignedToId is null or c.assignedTo.id = :assignedToId)")
    Page<CorrectiveAction> search(@Param("status") CorrectiveActionStatus status,
                                  @Param("assignedToId") Long assignedToId, Pageable pageable);

    @Query("select c from CorrectiveAction c join fetch c.finding f join fetch f.auditPlan p join fetch p.assignedAuditor "
            + "join fetch c.assignedTo join fetch c.createdBy where c.id = :id")
    Optional<CorrectiveAction> findDetailById(@Param("id") Long id);

    @Query("select c from CorrectiveAction c join fetch c.assignedTo where c.finding.id = :findingId order by c.deadline")
    List<CorrectiveAction> findByFindingId(@Param("findingId") Long findingId);

    long countByStatus(CorrectiveActionStatus status);

    long countByStatusIn(Collection<CorrectiveActionStatus> statuses);

    @Query("select count(c) from CorrectiveAction c where c.finding.id = :findingId "
            + "and c.status <> com.cybershield.enums.CorrectiveActionStatus.VERIFIED")
    long countUnverifiedForFinding(@Param("findingId") Long findingId);

    @Query("select count(c) from CorrectiveAction c where c.assignedTo.id = :userId "
            + "and c.status <> com.cybershield.enums.CorrectiveActionStatus.VERIFIED")
    long countOpenAssignedTo(@Param("userId") Long userId);

    @Query("select c from CorrectiveAction c join fetch c.finding f where c.status <> com.cybershield.enums.CorrectiveActionStatus.VERIFIED "
            + "order by c.deadline asc")
    List<CorrectiveAction> findOpenOrderByDeadline(Pageable pageable);
}

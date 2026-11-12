package com.cybershield.repository;

import com.cybershield.entity.RemediationEvidence;
import com.cybershield.enums.EvidenceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemediationEvidenceRepository extends JpaRepository<RemediationEvidence, Long> {

    @Query("select e from RemediationEvidence e join fetch e.submittedBy where e.task.id = :taskId "
            + "order by e.submittedAt asc, e.id asc")
    List<RemediationEvidence> findByTaskId(@Param("taskId") Long taskId);

    @Query("select e from RemediationEvidence e where e.task.id = :taskId and e.status = :status")
    List<RemediationEvidence> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") EvidenceStatus status);

    @Query("select e from RemediationEvidence e join fetch e.task t left join fetch t.assignedEngineer where e.id = :id")
    Optional<RemediationEvidence> findWithTaskById(@Param("id") Long id);

    long countByStatus(EvidenceStatus status);
}

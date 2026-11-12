package com.cybershield.repository;

import com.cybershield.entity.AuditEvidence;
import com.cybershield.enums.AuditEvidenceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEvidenceRepository extends JpaRepository<AuditEvidence, Long> {

    @Query("select e from AuditEvidence e join fetch e.submittedBy where e.correctiveAction.id = :actionId "
            + "order by e.submittedAt asc, e.id asc")
    List<AuditEvidence> findByActionId(@Param("actionId") Long actionId);

    @Query("select e from AuditEvidence e where e.correctiveAction.id = :actionId and e.status = :status")
    List<AuditEvidence> findByActionIdAndStatus(@Param("actionId") Long actionId,
                                                @Param("status") AuditEvidenceStatus status);

    @Query("select e from AuditEvidence e join fetch e.correctiveAction c join fetch c.assignedTo "
            + "join fetch e.submittedBy where e.id = :id")
    Optional<AuditEvidence> findWithActionById(@Param("id") Long id);

    long countByStatus(AuditEvidenceStatus status);
}

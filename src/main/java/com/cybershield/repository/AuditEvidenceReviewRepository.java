package com.cybershield.repository;

import com.cybershield.entity.AuditEvidenceReview;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Append-only: audit evidence review decisions are preserved. */
public interface AuditEvidenceReviewRepository extends Repository<AuditEvidenceReview, Long> {

    AuditEvidenceReview save(AuditEvidenceReview review);

    @Query("select r from AuditEvidenceReview r join fetch r.evidence e join fetch r.reviewer "
            + "where e.correctiveAction.id = :actionId order by r.reviewedAt asc, r.id asc")
    List<AuditEvidenceReview> findByActionId(@Param("actionId") Long actionId);
}

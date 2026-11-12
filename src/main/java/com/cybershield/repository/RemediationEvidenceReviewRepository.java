package com.cybershield.repository;

import com.cybershield.entity.RemediationEvidenceReview;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Append-only: review decisions are preserved forever. */
public interface RemediationEvidenceReviewRepository extends Repository<RemediationEvidenceReview, Long> {

    RemediationEvidenceReview save(RemediationEvidenceReview review);

    @Query("select r from RemediationEvidenceReview r join fetch r.evidence e join fetch r.reviewer "
            + "where e.task.id = :taskId order by r.reviewedAt asc, r.id asc")
    List<RemediationEvidenceReview> findByTaskId(@Param("taskId") Long taskId);
}

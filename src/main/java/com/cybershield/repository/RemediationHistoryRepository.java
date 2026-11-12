package com.cybershield.repository;

import com.cybershield.entity.RemediationHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Append-only repository: it extends the bare Repository interface and only declares save and read methods,
 * so the application has no way to update or delete history rows.
 */
public interface RemediationHistoryRepository extends Repository<RemediationHistory, Long> {

    RemediationHistory save(RemediationHistory history);

    @Query("select h from RemediationHistory h join fetch h.performedBy left join fetch h.previousAssignee "
            + "left join fetch h.newAssignee where h.task.id = :taskId order by h.createdAt asc, h.id asc")
    List<RemediationHistory> findByTaskId(@Param("taskId") Long taskId);

    @Query("select h from RemediationHistory h join fetch h.task t join fetch h.performedBy "
            + "where t.assignedEngineer.id = :userId or h.performedBy.id = :userId order by h.createdAt desc, h.id desc")
    List<RemediationHistory> findRecentForUser(@Param("userId") Long userId, Pageable pageable);

    long count();
}

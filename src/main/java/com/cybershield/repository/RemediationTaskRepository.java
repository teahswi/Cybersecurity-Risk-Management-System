package com.cybershield.repository;

import com.cybershield.entity.RemediationTask;
import com.cybershield.enums.Priority;
import com.cybershield.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemediationTaskRepository extends JpaRepository<RemediationTask, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(t.taskCode) like lower(concat('%', :q, '%')) "
            + "   or lower(t.title) like lower(concat('%', :q, '%')) or lower(a.assetCode) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or t.status = :status) "
            + "and (:priority is null or t.priority = :priority) "
            + "and (:engineerId is null or t.assignedEngineer.id = :engineerId) "
            + "and (:overdueBefore is null or (t.dueDate < :overdueBefore and t.status <> com.cybershield.enums.TaskStatus.COMPLETED))";

    @Query(value = "select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.asset a "
            + "join fetch av.vulnerability v left join fetch t.assignedEngineer " + SEARCH_WHERE,
            countQuery = "select count(t) from RemediationTask t join t.assetVulnerability av join av.asset a " + SEARCH_WHERE)
    Page<RemediationTask> search(@Param("q") String q, @Param("status") TaskStatus status,
                                 @Param("priority") Priority priority, @Param("engineerId") Long engineerId,
                                 @Param("overdueBefore") LocalDate overdueBefore, Pageable pageable);

    @Query("select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.asset a "
            + "join fetch a.department join fetch a.location join fetch av.vulnerability "
            + "left join fetch t.assignedEngineer join fetch t.createdBy where t.id = :id")
    Optional<RemediationTask> findDetailById(@Param("id") Long id);

    @Query("select t from RemediationTask t left join fetch t.assignedEngineer where t.assetVulnerability.id = :avId "
            + "order by t.createdAt desc")
    List<RemediationTask> findByAssetVulnerabilityId(@Param("avId") Long assetVulnerabilityId);

    @Query("select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.vulnerability "
            + "left join fetch t.assignedEngineer where av.asset.id = :assetId order by t.createdAt desc")
    List<RemediationTask> findByAssetId(@Param("assetId") Long assetId);

    @Query("select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.asset "
            + "left join fetch t.assignedEngineer where av.vulnerability.id = :vulnerabilityId order by t.createdAt desc")
    List<RemediationTask> findByVulnerabilityId(@Param("vulnerabilityId") Long vulnerabilityId);

    @Query("select count(t) from RemediationTask t where t.assetVulnerability.id = :avId "
            + "and t.status <> com.cybershield.enums.TaskStatus.COMPLETED")
    long countActiveForAssetVulnerability(@Param("avId") Long assetVulnerabilityId);

    /** Tasks per status (GROUP BY) - optionally only one engineer's tasks. Row: [TaskStatus, count]. */
    @Query("select t.status, count(t) from RemediationTask t where (:engineerId is null or t.assignedEngineer.id = :engineerId) "
            + "group by t.status")
    List<Object[]> countByStatus(@Param("engineerId") Long engineerId);

    @Query("select count(t) from RemediationTask t where t.dueDate < :today "
            + "and t.status <> com.cybershield.enums.TaskStatus.COMPLETED "
            + "and (:engineerId is null or t.assignedEngineer.id = :engineerId)")
    long countOverdue(@Param("today") LocalDate today, @Param("engineerId") Long engineerId);

    @Query("select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.asset "
            + "left join fetch t.assignedEngineer where t.dueDate between :from and :to "
            + "and t.status <> com.cybershield.enums.TaskStatus.COMPLETED "
            + "and (:engineerId is null or t.assignedEngineer.id = :engineerId) order by t.dueDate asc")
    List<RemediationTask> findDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                         @Param("engineerId") Long engineerId, Pageable pageable);

    /** Used by the deadline scheduler. */
    @Query("select t from RemediationTask t join fetch t.assignedEngineer where t.dueDate is not null "
            + "and t.dueDate <= :until and t.status in :statuses")
    List<RemediationTask> findAssignedDueOnOrBefore(@Param("until") LocalDate until,
                                                    @Param("statuses") Collection<TaskStatus> statuses);

    @Query("select count(t) from RemediationTask t where t.id = :taskId and t.assignedEngineer.id = :engineerId")
    long countAssignedTo(@Param("taskId") Long taskId, @Param("engineerId") Long engineerId);

    @Query("select t from RemediationTask t join fetch t.assetVulnerability av join fetch av.asset a join fetch av.vulnerability "
            + "left join fetch t.assignedEngineer where (:status is null or t.status = :status) "
            + "and (:engineerId is null or t.assignedEngineer.id = :engineerId) order by t.taskCode")
    List<RemediationTask> findForReport(@Param("status") TaskStatus status, @Param("engineerId") Long engineerId);

    @Query("select t from RemediationTask t join t.assetVulnerability av join av.asset a "
            + "where (lower(t.taskCode) like lower(concat('%', :q, '%')) or lower(t.title) like lower(concat('%', :q, '%')) "
            + "or lower(a.assetCode) like lower(concat('%', :q, '%'))) "
            + "and (:engineerId is null or t.assignedEngineer.id = :engineerId) order by t.taskCode")
    List<RemediationTask> quickSearch(@Param("q") String q, @Param("engineerId") Long engineerId, Pageable pageable);
}

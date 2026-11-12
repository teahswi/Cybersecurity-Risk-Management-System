package com.cybershield.repository;

import com.cybershield.entity.SecurityAlert;
import com.cybershield.enums.AlertSource;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.Severity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(s.alertCode) like lower(concat('%', :q, '%')) "
            + "   or lower(s.title) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or s.status = :status) and (:severity is null or s.severity = :severity) "
            + "and (:source is null or s.source = :source) "
            + "and (:analystId is null or s.assignedAnalyst.id = :analystId)";

    @Query(value = "select s from SecurityAlert s left join fetch s.asset left join fetch s.assignedAnalyst " + SEARCH_WHERE,
            countQuery = "select count(s) from SecurityAlert s " + SEARCH_WHERE)
    Page<SecurityAlert> search(@Param("q") String q, @Param("status") AlertStatus status,
                               @Param("severity") Severity severity, @Param("source") AlertSource source,
                               @Param("analystId") Long analystId, Pageable pageable);

    @Query("select s from SecurityAlert s left join fetch s.asset left join fetch s.assignedAnalyst "
            + "left join fetch s.createdBy where s.id = :id")
    Optional<SecurityAlert> findDetailById(@Param("id") Long id);

    long countByStatusIn(Collection<AlertStatus> statuses);

    @Query("select s from SecurityAlert s where s.status in :statuses order by s.detectedAt desc")
    List<SecurityAlert> findByStatusIn(@Param("statuses") Collection<AlertStatus> statuses, Pageable pageable);

    @Query("select s from SecurityAlert s left join fetch s.assignedAnalyst where s.asset.id = :assetId order by s.detectedAt desc")
    List<SecurityAlert> findByAssetId(@Param("assetId") Long assetId);

    @Query("select s from SecurityAlert s left join fetch s.asset where s.status in :statuses order by s.detectedAt desc")
    List<SecurityAlert> findRecentByStatusIn(@Param("statuses") Collection<AlertStatus> statuses, Pageable pageable);
}

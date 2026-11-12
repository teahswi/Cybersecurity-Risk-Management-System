package com.cybershield.repository;

import com.cybershield.entity.RiskAssessment;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(r.riskCode) like lower(concat('%', :q, '%')) "
            + "or lower(r.title) like lower(concat('%', :q, '%'))) "
            + "and (:level is null or r.riskLevel = :level) and (:status is null or r.status = :status)";

    @Query(value = "select r from RiskAssessment r join fetch r.asset left join fetch r.owner " + SEARCH_WHERE,
            countQuery = "select count(r) from RiskAssessment r " + SEARCH_WHERE)
    Page<RiskAssessment> search(@Param("q") String q, @Param("level") RiskLevel level,
                                @Param("status") RiskStatus status, Pageable pageable);

    @Query("select r from RiskAssessment r join fetch r.asset left join fetch r.assetVulnerability av "
            + "left join fetch av.vulnerability left join fetch r.owner join fetch r.createdBy where r.id = :id")
    Optional<RiskAssessment> findDetailById(@Param("id") Long id);

    @Query("select r from RiskAssessment r left join fetch r.owner where r.asset.id = :assetId order by r.riskScore desc")
    List<RiskAssessment> findByAssetId(@Param("assetId") Long assetId);

    @Query("select r from RiskAssessment r where r.assetVulnerability.id = :avId order by r.riskScore desc")
    List<RiskAssessment> findByAssetVulnerabilityId(@Param("avId") Long assetVulnerabilityId);

    /** Open risks per level for the dashboard chart. Row: [RiskLevel, count]. */
    @Query("select r.riskLevel, count(r) from RiskAssessment r where r.status <> com.cybershield.enums.RiskStatus.CLOSED "
            + "group by r.riskLevel")
    List<Object[]> countOpenByLevel();

    @Query("select r from RiskAssessment r join fetch r.asset where r.status <> com.cybershield.enums.RiskStatus.CLOSED "
            + "order by r.riskScore desc, r.updatedAt desc")
    List<RiskAssessment> findTopOpen(Pageable pageable);

    @Query("select r from RiskAssessment r join fetch r.asset left join fetch r.owner "
            + "where (:level is null or r.riskLevel = :level) and (:status is null or r.status = :status) "
            + "order by r.riskScore desc")
    List<RiskAssessment> findForReport(@Param("level") RiskLevel level, @Param("status") RiskStatus status);
}

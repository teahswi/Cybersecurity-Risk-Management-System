package com.cybershield.repository;

import com.cybershield.entity.Asset;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.Environment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(a.assetCode) like lower(concat('%', :q, '%')) "
            + "   or lower(a.name) like lower(concat('%', :q, '%')) or lower(a.hostname) like lower(concat('%', :q, '%')) "
            + "   or a.ipAddress like concat('%', :q, '%')) "
            + "and (:departmentId is null or a.department.id = :departmentId) "
            + "and (:locationId is null or a.location.id = :locationId) "
            + "and (:type is null or a.assetType = :type) "
            + "and (:environment is null or a.environment = :environment) "
            + "and ((:status is null and a.status <> com.cybershield.enums.AssetStatus.ARCHIVED) or a.status = :status) "
            // engineers only see assets they own or have (had) a remediation task on
            + "and (:restrictToUserId is null or a.owner.id = :restrictToUserId or a.id in "
            + "   (select t.assetVulnerability.asset.id from RemediationTask t where t.assignedEngineer.id = :restrictToUserId))";

    @Query(value = "select a from Asset a join fetch a.department join fetch a.location left join fetch a.owner "
            + SEARCH_WHERE,
            countQuery = "select count(a) from Asset a " + SEARCH_WHERE)
    Page<Asset> search(@Param("q") String q, @Param("departmentId") Long departmentId,
                       @Param("locationId") Long locationId, @Param("type") AssetType type,
                       @Param("environment") Environment environment, @Param("status") AssetStatus status,
                       @Param("restrictToUserId") Long restrictToUserId, Pageable pageable);

    @Query("select a from Asset a join fetch a.department join fetch a.location left join fetch a.owner where a.id = :id")
    Optional<Asset> findDetailById(@Param("id") Long id);

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    boolean existsByAssetCodeIgnoreCaseAndIdNot(String assetCode, Long id);

    List<Asset> findByStatusNotOrderByAssetCodeAsc(AssetStatus status);

    long countByStatusNot(AssetStatus status);

    @Query("select count(a) from Asset a where a.id = :assetId and (a.owner.id = :userId or a.id in "
            + "(select t.assetVulnerability.asset.id from RemediationTask t where t.assignedEngineer.id = :userId))")
    long countRelatedToEngineer(@Param("assetId") Long assetId, @Param("userId") Long userId);

    /** Asset distribution by type for the dashboard chart (GROUP BY). Row: [AssetType, count]. */
    @Query("select a.assetType, count(a) from Asset a where a.status <> com.cybershield.enums.AssetStatus.ARCHIVED "
            + "group by a.assetType order by count(a) desc")
    List<Object[]> countByType();

    @Query("select a from Asset a join fetch a.department join fetch a.location left join fetch a.owner "
            + "where (:departmentId is null or a.department.id = :departmentId) "
            + "and (:status is null or a.status = :status) order by a.assetCode")
    List<Asset> findForReport(@Param("departmentId") Long departmentId, @Param("status") AssetStatus status);

    @Query("select a from Asset a where lower(a.assetCode) like lower(concat('%', :q, '%')) "
            + "or lower(a.name) like lower(concat('%', :q, '%')) or lower(a.hostname) like lower(concat('%', :q, '%')) "
            + "or a.ipAddress like concat('%', :q, '%') order by a.assetCode")
    List<Asset> quickSearch(@Param("q") String q, Pageable pageable);
}

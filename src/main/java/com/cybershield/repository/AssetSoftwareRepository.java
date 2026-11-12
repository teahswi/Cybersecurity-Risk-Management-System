package com.cybershield.repository;

import com.cybershield.entity.AssetSoftware;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetSoftwareRepository extends JpaRepository<AssetSoftware, Long> {

    @Query("select x from AssetSoftware x join fetch x.software where x.asset.id = :assetId order by x.software.name")
    List<AssetSoftware> findByAssetId(@Param("assetId") Long assetId);

    @Query("select x from AssetSoftware x join fetch x.asset a join fetch a.department join fetch a.location "
            + "where x.software.id = :softwareId order by a.assetCode")
    List<AssetSoftware> findBySoftwareId(@Param("softwareId") Long softwareId);

    boolean existsByAssetIdAndSoftwareId(Long assetId, Long softwareId);

    /** Installation count per software product (GROUP BY). Row: [softwareId, count]. */
    @Query("select x.software.id, count(x) from AssetSoftware x where x.software.id in :ids group by x.software.id")
    List<Object[]> countInstallations(@Param("ids") List<Long> softwareIds);
}

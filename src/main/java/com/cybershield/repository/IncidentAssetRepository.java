package com.cybershield.repository;

import com.cybershield.entity.IncidentAsset;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IncidentAssetRepository extends Repository<IncidentAsset, Long> {

    IncidentAsset save(IncidentAsset link);

    boolean existsByIncidentIdAndAssetId(Long incidentId, Long assetId);

    @Query("select x from IncidentAsset x join fetch x.asset a join fetch a.department where x.incident.id = :incidentId "
            + "order by a.assetCode")
    List<IncidentAsset> findByIncidentId(@Param("incidentId") Long incidentId);

    @Query("select x from IncidentAsset x join fetch x.incident i left join fetch i.assignedAnalyst "
            + "where x.asset.id = :assetId order by i.detectedAt desc")
    List<IncidentAsset> findByAssetId(@Param("assetId") Long assetId);
}

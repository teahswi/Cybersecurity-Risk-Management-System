package com.cybershield.repository;

import com.cybershield.entity.IncidentAlert;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IncidentAlertRepository extends Repository<IncidentAlert, Long> {

    IncidentAlert save(IncidentAlert link);

    boolean existsByIncidentIdAndAlertId(Long incidentId, Long alertId);

    @Query("select x from IncidentAlert x join fetch x.alert a left join fetch a.asset where x.incident.id = :incidentId "
            + "order by a.detectedAt desc")
    List<IncidentAlert> findByIncidentId(@Param("incidentId") Long incidentId);

    @Query("select x from IncidentAlert x join fetch x.incident where x.alert.id = :alertId order by x.linkedAt desc")
    List<IncidentAlert> findByAlertId(@Param("alertId") Long alertId);
}

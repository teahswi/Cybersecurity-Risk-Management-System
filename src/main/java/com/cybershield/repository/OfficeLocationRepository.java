package com.cybershield.repository;

import com.cybershield.entity.OfficeLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfficeLocationRepository extends JpaRepository<OfficeLocation, Long> {

    List<OfficeLocation> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Number of assets per office (LEFT JOIN + GROUP BY). Row: [locationId, count]. */
    @Query("select l.id, count(a.id) from OfficeLocation l left join Asset a on a.location = l "
            + "and a.status <> com.cybershield.enums.AssetStatus.ARCHIVED group by l.id")
    List<Object[]> countAssetsPerLocation();
}

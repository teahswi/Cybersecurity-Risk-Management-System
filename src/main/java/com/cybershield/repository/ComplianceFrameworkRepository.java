package com.cybershield.repository;

import com.cybershield.entity.ComplianceFramework;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComplianceFrameworkRepository extends JpaRepository<ComplianceFramework, Long> {

    List<ComplianceFramework> findAllByOrderByNameAsc();

    /** Controls per framework (GROUP BY). Row: [frameworkId, count]. */
    @Query("select c.framework.id, count(c) from ComplianceControl c group by c.framework.id")
    List<Object[]> countControls();
}

package com.cybershield.repository;

import com.cybershield.entity.RiskTreatment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskTreatmentRepository extends JpaRepository<RiskTreatment, Long> {

    @Query("select t from RiskTreatment t left join fetch t.owner where t.risk.id = :riskId order by t.createdAt asc")
    List<RiskTreatment> findByRiskId(@Param("riskId") Long riskId);
}

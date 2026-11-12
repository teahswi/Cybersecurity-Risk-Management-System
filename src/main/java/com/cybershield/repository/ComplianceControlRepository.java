package com.cybershield.repository;

import com.cybershield.entity.ComplianceControl;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplianceControlRepository extends JpaRepository<ComplianceControl, Long> {

    @Query(value = "select c from ComplianceControl c join fetch c.framework f "
            + "where (:frameworkId is null or f.id = :frameworkId) and (:q is null or lower(c.controlCode) like lower(concat('%', :q, '%')) "
            + "or lower(c.title) like lower(concat('%', :q, '%')))",
            countQuery = "select count(c) from ComplianceControl c where (:frameworkId is null or c.framework.id = :frameworkId) "
            + "and (:q is null or lower(c.controlCode) like lower(concat('%', :q, '%')) or lower(c.title) like lower(concat('%', :q, '%')))")
    Page<ComplianceControl> search(@Param("frameworkId") Long frameworkId, @Param("q") String q, Pageable pageable);

    @Query("select c from ComplianceControl c where c.framework.id = :frameworkId order by c.controlCode")
    List<ComplianceControl> findByFrameworkId(@Param("frameworkId") Long frameworkId);

    boolean existsByFrameworkIdAndControlCodeIgnoreCase(Long frameworkId, String controlCode);
}

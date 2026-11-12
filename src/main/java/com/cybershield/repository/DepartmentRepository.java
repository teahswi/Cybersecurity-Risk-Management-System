package com.cybershield.repository;

import com.cybershield.entity.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Number of assets per department (LEFT JOIN + GROUP BY). Row: [departmentId, count]. */
    @Query("select d.id, count(a.id) from Department d left join Asset a on a.department = d "
            + "and a.status <> com.cybershield.enums.AssetStatus.ARCHIVED group by d.id")
    List<Object[]> countAssetsPerDepartment();

    @Query("select count(u) from User u where u.department.id = :id")
    long countUsers(@Param("id") Long id);
}

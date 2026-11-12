package com.cybershield.repository;

import com.cybershield.entity.Software;
import com.cybershield.enums.SoftwareCategory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SoftwareRepository extends JpaRepository<Software, Long> {

    @Query("select s from Software s where (:q is null or lower(s.name) like lower(concat('%', :q, '%')) "
            + "or lower(s.vendor) like lower(concat('%', :q, '%'))) and (:category is null or s.category = :category)")
    Page<Software> search(@Param("q") String q, @Param("category") SoftwareCategory category, Pageable pageable);

    boolean existsByNameIgnoreCaseAndVendorIgnoreCase(String name, String vendor);

    boolean existsByNameIgnoreCaseAndVendorIgnoreCaseAndIdNot(String name, String vendor, Long id);

    List<Software> findAllByOrderByNameAsc();
}

package com.cybershield.repository;

import com.cybershield.entity.Incident;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.IncidentType;
import com.cybershield.enums.Severity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    String SEARCH_WHERE = "where (:q is null or lower(i.incidentCode) like lower(concat('%', :q, '%')) "
            + "   or lower(i.title) like lower(concat('%', :q, '%'))) "
            + "and (:status is null or i.status = :status) and (:severity is null or i.severity = :severity) "
            + "and (:type is null or i.incidentType = :type)";

    @Query(value = "select i from Incident i left join fetch i.assignedAnalyst " + SEARCH_WHERE,
            countQuery = "select count(i) from Incident i " + SEARCH_WHERE)
    Page<Incident> search(@Param("q") String q, @Param("status") IncidentStatus status,
                          @Param("severity") Severity severity, @Param("type") IncidentType type, Pageable pageable);

    @Query("select i from Incident i left join fetch i.assignedAnalyst join fetch i.createdBy where i.id = :id")
    Optional<Incident> findDetailById(@Param("id") Long id);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    /** Incidents per status for the dashboard chart. Row: [IncidentStatus, count]. */
    @Query("select i.status, count(i) from Incident i group by i.status")
    List<Object[]> countByStatus();

    @Query("select i from Incident i left join fetch i.assignedAnalyst order by i.detectedAt desc")
    List<Incident> findRecent(Pageable pageable);

    @Query("select i from Incident i left join fetch i.assignedAnalyst "
            + "where (:status is null or i.status = :status) and (:from is null or i.detectedAt >= :from) "
            + "and (:to is null or i.detectedAt < :to) order by i.detectedAt desc")
    List<Incident> findForReport(@Param("status") IncidentStatus status, @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    @Query("select i from Incident i where lower(i.incidentCode) like lower(concat('%', :q, '%')) "
            + "or lower(i.title) like lower(concat('%', :q, '%')) order by i.incidentCode")
    List<Incident> quickSearch(@Param("q") String q, Pageable pageable);
}

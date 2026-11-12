package com.cybershield.repository;

import com.cybershield.entity.InvestigationNote;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InvestigationNoteRepository extends Repository<InvestigationNote, Long> {

    InvestigationNote save(InvestigationNote note);

    @Query("select n from InvestigationNote n join fetch n.author where n.incident.id = :incidentId "
            + "order by n.createdAt asc, n.id asc")
    List<InvestigationNote> findByIncidentId(@Param("incidentId") Long incidentId);
}

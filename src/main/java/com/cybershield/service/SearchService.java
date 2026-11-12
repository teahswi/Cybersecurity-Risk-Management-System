package com.cybershield.service;

import com.cybershield.dto.SearchResult;
import com.cybershield.enums.RoleCode;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.VulnerabilityRepository;
import com.cybershield.security.CurrentUserService;
import com.cybershield.security.SecurityUser;
import com.cybershield.util.Texts;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Navbar global search across assets, vulnerabilities, incidents and remediation tasks.
 * Filtering is done on the SERVER by role, so users never receive results they may not open.
 */
@Service
public class SearchService {

    public static final int PER_CATEGORY = 6;

    private final AssetRepository assetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final IncidentRepository incidentRepository;
    private final RemediationTaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public SearchService(AssetRepository assetRepository, VulnerabilityRepository vulnerabilityRepository,
                         IncidentRepository incidentRepository, RemediationTaskRepository taskRepository,
                         CurrentUserService currentUserService) {
        this.assetRepository = assetRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.incidentRepository = incidentRepository;
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query) {
        String q = Texts.clean(query);
        List<SearchResult> results = new ArrayList<>();
        if (q == null || q.length() < 2) {
            return results;
        }
        q = Texts.truncate(q, 100);
        SecurityUser me = currentUserService.require();
        RoleCode role = me.getRoleCode();
        boolean engineer = role == RoleCode.IT_INFRASTRUCTURE_ENGINEER;
        boolean securityOps = role == RoleCode.SECURITY_ANALYST || role == RoleCode.SECURITY_MANAGER;
        PageRequest limit = PageRequest.of(0, PER_CATEGORY);

        // Assets: everyone, but engineers only assets related to them
        assetRepository.quickSearch(q, PageRequest.of(0, 50)).stream()
                .filter(a -> !engineer || assetRepository.countRelatedToEngineer(a.getId(), me.getId()) > 0)
                .limit(PER_CATEGORY)
                .forEach(a -> results.add(new SearchResult("Asset", a.getAssetCode(), a.getName(), a.getStatus().getLabel(),
                        "/assets/" + a.getId(), "bi-hdd-network")));

        if (securityOps || engineer) {
            vulnerabilityRepository.quickSearch(q, PageRequest.of(0, 50)).stream()
                    .filter(v -> !engineer || vulnerabilityRepository.countRelatedToEngineer(v.getId(), me.getId()) > 0)
                    .limit(PER_CATEGORY)
                    .forEach(v -> results.add(new SearchResult("Vulnerability", v.getVulnCode(), v.getTitle(),
                            v.getSeverity().getLabel(), "/vulnerabilities/" + v.getId(), "bi-bug")));
            taskRepository.quickSearch(q, engineer ? me.getId() : null, limit)
                    .forEach(t -> results.add(new SearchResult("Remediation task", t.getTaskCode(), t.getTitle(),
                            t.getStatus().getLabel(), "/remediation/" + t.getId(), "bi-tools")));
        }
        if (securityOps) {
            incidentRepository.quickSearch(q, limit)
                    .forEach(i -> results.add(new SearchResult("Incident", i.getIncidentCode(), i.getTitle(),
                            i.getStatus().getLabel(), "/incidents/" + i.getId(), "bi-fire")));
        }
        return results;
    }
}

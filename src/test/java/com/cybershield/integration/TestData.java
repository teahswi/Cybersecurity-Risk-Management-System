package com.cybershield.integration;

import com.cybershield.entity.AssetVulnerability;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.AuditFindingRepository;
import com.cybershield.repository.CorrectiveActionRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.SecurityAlertRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.repository.VulnerabilityRepository;
import org.springframework.stereotype.Component;

/** Looks up seeded demo records by their business codes. */
@Component
public class TestData {

    private final AssetRepository assets;
    private final VulnerabilityRepository vulnerabilities;
    private final AssetVulnerabilityRepository assetVulnerabilities;
    private final RemediationTaskRepository tasks;
    private final SecurityAlertRepository alerts;
    private final IncidentRepository incidents;
    private final AuditFindingRepository findings;
    private final CorrectiveActionRepository actions;
    private final UserRepository users;

    public TestData(AssetRepository assets, VulnerabilityRepository vulnerabilities,
                    AssetVulnerabilityRepository assetVulnerabilities, RemediationTaskRepository tasks,
                    SecurityAlertRepository alerts, IncidentRepository incidents, AuditFindingRepository findings,
                    CorrectiveActionRepository actions, UserRepository users) {
        this.assets = assets;
        this.vulnerabilities = vulnerabilities;
        this.assetVulnerabilities = assetVulnerabilities;
        this.tasks = tasks;
        this.alerts = alerts;
        this.incidents = incidents;
        this.findings = findings;
        this.actions = actions;
        this.users = users;
    }

    public Long assetId(String code) {
        return assets.findAll().stream().filter(a -> a.getAssetCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public Long vulnerabilityId(String code) {
        return vulnerabilities.findAll().stream().filter(v -> v.getVulnCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public AssetVulnerability instance(String assetCode, String vulnCode) {
        return assetVulnerabilities.findAll().stream()
                .filter(av -> av.getAsset().getAssetCode().equals(assetCode)
                        && av.getVulnerability().getVulnCode().equals(vulnCode))
                .findFirst().orElseThrow();
    }

    public Long taskId(String code) {
        return tasks.findAll().stream().filter(t -> t.getTaskCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public Long alertId(String code) {
        return alerts.findAll().stream().filter(a -> a.getAlertCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public Long incidentId(String code) {
        return incidents.findAll().stream().filter(i -> i.getIncidentCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public Long findingId(String code) {
        return findings.findAll().stream().filter(f -> f.getFindingCode().equals(code)).findFirst().orElseThrow().getId();
    }

    public Long firstActionOfFinding(String findingCode) {
        return actions.findByFindingId(findingId(findingCode)).getFirst().getId();
    }

    public Long userId(String username) {
        return users.findByUsernameIgnoreCase(username).orElseThrow().getId();
    }
}

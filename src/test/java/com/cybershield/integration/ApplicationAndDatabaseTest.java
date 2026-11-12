package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cybershield.entity.AssetVulnerability;
import com.cybershield.entity.IncidentAlert;
import com.cybershield.repository.AssetRepository;
import com.cybershield.repository.AssetVulnerabilityRepository;
import com.cybershield.repository.IncidentAlertRepository;
import com.cybershield.repository.IncidentRepository;
import com.cybershield.repository.RoleRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.service.SystemService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Tests 1, 2, 3 and 5: context start-up, database connectivity, entity relationships and BCrypt. */
class ApplicationAndDatabaseTest extends IntegrationTestBase {

    @Autowired
    ApplicationContext context;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    SystemService systemService;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AssetRepository assetRepository;
    @Autowired
    AssetVulnerabilityRepository assetVulnerabilityRepository;
    @Autowired
    IncidentRepository incidentRepository;
    @Autowired
    IncidentAlertRepository incidentAlertRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void applicationContextStarts() {
        assertThat(context.getBean(SystemService.class)).isNotNull();
        assertThat(context.containsBean("demoDataInitializer")).as("demo initializer is dev-profile only").isFalse();
    }

    @Test
    void databaseIsReachableAndMigrated() {
        assertThat(systemService.isDatabaseUp()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
        Integer migrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL", Integer.class);
        assertThat(migrations).isGreaterThanOrEqualTo(10);
        assertThat(roleRepository.count()).isEqualTo(4);
        Integer views = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.views "
                + "WHERE table_schema = DATABASE()", Integer.class);
        assertThat(views).isEqualTo(2);
    }

    @Test
    void entityRelationshipsAreNavigable() {
        Long assetId = assetRepository.findAll().stream().filter(a -> a.getAssetCode().equals("SRV-104"))
                .findFirst().orElseThrow().getId();
        List<AssetVulnerability> instances = assetVulnerabilityRepository.findByAssetId(assetId);
        assertThat(instances).hasSize(1);
        AssetVulnerability av = instances.getFirst();
        assertThat(av.getAsset().getName()).isEqualTo("Finance Production Server");
        assertThat(av.getAsset().getDepartment().getName()).isEqualTo("Finance");
        assertThat(av.getVulnerability().getCveId()).as("demo data never uses a real CVE").isNull();
        assertThat(av.getVulnerability().getTitle()).startsWith("[DEMO]");
        // the same vulnerability definition affects more than one asset (many-to-many through asset_vulnerabilities)
        assertThat(assetVulnerabilityRepository.findByVulnerabilityId(av.getVulnerability().getId())).hasSizeGreaterThan(1);

        Long incidentId = incidentRepository.findAll().stream().filter(i -> i.getIncidentCode().equals("INC-0001"))
                .findFirst().orElseThrow().getId();
        List<IncidentAlert> links = incidentAlertRepository.findByIncidentId(incidentId);
        assertThat(links).extracting(l -> l.getAlert().getAlertCode()).contains("ALT-0002", "ALT-0004");
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        var manager = userRepository.findByUsernameIgnoreCase("manager").orElseThrow();
        assertThat(manager.getPasswordHash()).startsWith("$2").hasSize(60).isNotEqualTo("Manager@123");
        assertThat(passwordEncoder.matches("Manager@123", manager.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("wrong", manager.getPasswordHash())).isFalse();
    }
}

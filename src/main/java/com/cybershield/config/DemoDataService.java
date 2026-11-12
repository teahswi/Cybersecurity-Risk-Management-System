package com.cybershield.config;

import com.cybershield.entity.User;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.RoleRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.storage.FileStorageService;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent demo data loader:
 *  1. creates any missing demo users (BCrypt-hashed passwords, never overwrites an existing user),
 *  2. runs demo/demo-data.sql, whose statements only insert rows that do not exist yet,
 *  3. writes the small sample evidence files referenced by the demo evidence rows (if missing).
 * Called by {@link DemoDataInitializer} in the dev profile and by integration tests.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    /** username, password, full name, role, department code, job title */
    public static final List<String[]> DEMO_USERS = List.of(
            new String[]{"analyst", "Analyst@123", "Ananya Rao", "SECURITY_ANALYST", "SEC", "Security Analyst"},
            new String[]{"engineer", "Engineer@123", "Vikram Singh", "IT_INFRASTRUCTURE_ENGINEER", "IT", "Infrastructure Engineer"},
            new String[]{"auditor", "Auditor@123", "Meera Iyer", "COMPLIANCE_AUDITOR", "LEG", "Compliance Auditor"},
            new String[]{"manager", "Manager@123", "Arjun Mehta", "SECURITY_MANAGER", "SEC", "Security Manager"},
            new String[]{"analyst2", "Demo@12345", "Neha Kapoor", "SECURITY_ANALYST", "SEC", "Senior Security Analyst"},
            new String[]{"engineer2", "Demo@12345", "Rahul Verma", "IT_INFRASTRUCTURE_ENGINEER", "IT", "Network Engineer"});

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final FileStorageService fileStorageService;

    public DemoDataService(UserRepository userRepository, RoleRepository roleRepository,
                           DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
                           DataSource dataSource, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataSource = dataSource;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public int seed() {
        int created = 0;
        for (String[] d : DEMO_USERS) {
            if (userRepository.existsByUsernameIgnoreCase(d[0])) {
                continue; // never overwrite an existing user's password or profile
            }
            User u = new User();
            u.setUsername(d[0]);
            u.setPasswordHash(passwordEncoder.encode(d[1]));
            u.setFullName(d[2]);
            u.setEmail(d[0] + "@cybershield.local");
            u.setRole(roleRepository.findByCode(RoleCode.valueOf(d[3]))
                    .orElseThrow(() -> new IllegalStateException("Role missing - did Flyway run?")));
            u.setDepartment(departmentRepository.findAll().stream().filter(dep -> dep.getCode().equals(d[4]))
                    .findFirst().orElse(null));
            u.setJobTitle(d[5]);
            u.setStatus(UserStatus.ACTIVE);
            userRepository.save(u);
            created++;
        }
        userRepository.flush();

        // Each statement in this script is "INSERT ... SELECT ... WHERE NOT EXISTS", so it is safe to re-run.
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("demo/demo-data.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);

        fileStorageService.writeTextFileIfMissing("remediation/demo-rem-0003-db201-patch-report.txt",
                "SAMPLE DEMONSTRATION EVIDENCE - REM-0003\n"
                        + "Host: DB-201 (Finance ERP Database)\n"
                        + "Action: quarterly database patch bundle applied during the maintenance window.\n"
                        + "Result: service restarted, version check shows the patched build. No errors in the log.\n");
        fileStorageService.writeTextFileIfMissing("remediation/demo-rem-0004-net402-credential-change.txt",
                "SAMPLE DEMONSTRATION EVIDENCE - REM-0004\n"
                        + "Device: NET-402 (Branch Router Pune)\n"
                        + "Action: factory default administrator password replaced; management restricted to admin VLAN.\n"
                        + "Verification: login attempt with default credentials was rejected.\n");
        fileStorageService.writeTextFileIfMissing("audit/demo-audit-log-retention-policy.txt",
                "SAMPLE DEMONSTRATION EVIDENCE - FND-0002\n"
                        + "Draft security log retention standard: security event logs are kept for 12 months online "
                        + "and 24 months in archive.\n");
        fileStorageService.writeTextFileIfMissing("audit/demo-audit-leaver-review.txt",
                "SAMPLE DEMONSTRATION EVIDENCE - FND-0003\n"
                        + "Leaver account review completed. Three stale HR application accounts were disabled.\n");
        log.info("Demo data check complete: {} demo user(s) created, sample records ensured.", created);
        return created;
    }
}

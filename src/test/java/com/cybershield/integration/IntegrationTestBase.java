package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cybershield.config.DemoDataService;
import com.cybershield.security.SecurityUser;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Base class for integration tests against the real MySQL test database (cybershield_test).
 *
 * - Flyway migrates cybershield_test when the Spring context starts.
 * - The idempotent demo data is loaded once (committed) so every test sees SRV-104 etc.
 * - Each test runs in a transaction that is ROLLED BACK afterwards, so tests never leave changes behind.
 * - A safety check refuses to run against any database other than cybershield_test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    private static boolean seeded = false;

    @Autowired
    protected DemoDataService demoDataService;
    @Autowired
    protected PlatformTransactionManager transactionManager;
    @Autowired
    protected UserDetailsService userDetailsService;
    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    void prepareDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog())
                    .as("Integration tests must only run against the cybershield_test database")
                    .isEqualTo("cybershield_test");
        }
        synchronized (IntegrationTestBase.class) {
            if (!seeded) {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.executeWithoutResult(status -> demoDataService.seed());
                seeded = true;
            }
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Authenticates the given demo user for direct service calls (same as a real login). */
    protected SecurityUser loginAs(String username) {
        UserDetails user = userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        return (SecurityUser) user;
    }

    protected SecurityUser principal(String username) {
        return (SecurityUser) userDetailsService.loadUserByUsername(username);
    }
}

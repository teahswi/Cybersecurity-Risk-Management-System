package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cybershield.dto.AssetForm;
import com.cybershield.entity.User;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.AuditAction;
import com.cybershield.enums.Criticality;
import com.cybershield.enums.Environment;
import com.cybershield.enums.ReviewDecision;
import com.cybershield.enums.UserStatus;
import com.cybershield.repository.DepartmentRepository;
import com.cybershield.repository.OfficeLocationRepository;
import com.cybershield.repository.RemediationTaskRepository;
import com.cybershield.repository.UserRepository;
import com.cybershield.service.AssetService;
import com.cybershield.service.RemediationService;
import com.cybershield.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Tests 4, 6, 7 and 25: login, role-based page access, service-level authorization and login audit logs. */
class AuthenticationAndAuthorizationTest extends IntegrationTestBase {

    @Autowired
    WebApplicationContext webContext;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AssetService assetService;
    @Autowired
    RemediationService remediationService;
    @Autowired
    UserService userService;
    @Autowired
    RemediationTaskRepository taskRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    OfficeLocationRepository locationRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    void allFourDemoAccountsCanLogIn() throws Exception {
        String[][] accounts = {{"analyst", "Analyst@123"}, {"engineer", "Engineer@123"},
                {"auditor", "Auditor@123"}, {"manager", "Manager@123"}};
        for (String[] a : accounts) {
            mvc.perform(formLogin("/login").user(a[0]).password(a[1]))
                    .andExpect(authenticated().withUsername(a[0]))
                    .andExpect(redirectedUrl("/dashboard"));
        }
    }

    @Test
    void wrongPasswordIsRejectedAndAudited() throws Exception {
        long before = countAudit(AuditAction.LOGIN_FAILURE);
        mvc.perform(formLogin("/login").user("analyst").password("not-the-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").user("nobody-" + System.nanoTime()).password("x"))
                .andExpect(unauthenticated());
        assertThat(countAudit(AuditAction.LOGIN_FAILURE)).isEqualTo(before + 2);
        Integer nullUserRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = 'LOGIN_FAILURE' "
                + "AND user_id IS NULL AND username LIKE 'nobody-%'", Integer.class);
        assertThat(nullUserRows).as("unknown usernames are logged with a NULL user reference").isGreaterThanOrEqualTo(1);
    }

    @Test
    void successfulLoginAndLogoutAreAudited() throws Exception {
        long logins = countAudit(AuditAction.LOGIN_SUCCESS);
        long logouts = countAudit(AuditAction.LOGOUT);
        mvc.perform(formLogin("/login").user("auditor").password("Auditor@123")).andExpect(authenticated());
        mvc.perform(post("/logout").with(user(principal("auditor"))).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(countAudit(AuditAction.LOGIN_SUCCESS)).isEqualTo(logins + 1);
        assertThat(countAudit(AuditAction.LOGOUT)).isEqualTo(logouts + 1);
    }

    @Test
    void inactiveAccountCannotLogIn() throws Exception {
        User engineer2 = userRepository.findByUsernameIgnoreCase("engineer2").orElseThrow();
        engineer2.setStatus(UserStatus.INACTIVE);
        userRepository.flush();
        mvc.perform(formLogin("/login").user("engineer2").password("Demo@12345"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?disabled"));
    }

    @Test
    void anonymousUsersAreRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void pagesAreProtectedByRole() throws Exception {
        mvc.perform(get("/users").with(user(principal("engineer")))).andExpect(status().isForbidden());
        mvc.perform(get("/users").with(user(principal("analyst")))).andExpect(status().isForbidden());
        mvc.perform(get("/users").with(user(principal("manager")))).andExpect(status().isOk());
        mvc.perform(get("/alerts").with(user(principal("auditor")))).andExpect(status().isForbidden());
        mvc.perform(get("/alerts").with(user(principal("analyst")))).andExpect(status().isOk());
        mvc.perform(get("/audits").with(user(principal("engineer")))).andExpect(status().isForbidden());
        mvc.perform(get("/audits").with(user(principal("auditor")))).andExpect(status().isOk());
        mvc.perform(get("/system").with(user(principal("analyst")))).andExpect(status().isForbidden());
        mvc.perform(get("/system/audit-logs").with(user(principal("manager")))).andExpect(status().isOk());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/notifications/read-all").with(user(principal("analyst")))).andExpect(status().isForbidden());
    }

    @Test
    void servicesRejectUnauthorizedOperations() {
        loginAs("engineer");
        assertThatThrownBy(() -> assetService.create(sampleAsset("ENG-TRY-1"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userService.search(null, null, null, org.springframework.data.domain.PageRequest.of(0, 5)))
                .isInstanceOf(AccessDeniedException.class);

        loginAs("auditor");
        Long rem3 = taskIdByCode("REM-0003");
        assertThatThrownBy(() -> remediationService.reviewEvidence(rem3, ReviewDecision.APPROVED, "ok"))
                .as("an auditor cannot approve remediation evidence")
                .isInstanceOf(AccessDeniedException.class);

        // engineer2 cannot modify a task assigned to engineer
        loginAs("engineer2");
        Long rem2 = taskIdByCode("REM-0002");
        assertThatThrownBy(() -> remediationService.startWork(rem2)).isInstanceOf(AccessDeniedException.class);

        // the hidden-button rule: analysts cannot create assets even by calling the service directly
        loginAs("analyst");
        assertThatThrownBy(() -> assetService.create(sampleAsset("ANL-TRY-1"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void managerCannotRemoveTheLastActiveManagerOrChangeOwnRole() {
        var me = loginAs("manager");
        var form = userService.formFor(me.getId());
        form.setRole(com.cybershield.enums.RoleCode.SECURITY_ANALYST);
        assertThatThrownBy(() -> userService.update(me.getId(), form))
                .hasMessageContaining("own role");
        assertThatThrownBy(() -> userService.setActive(me.getId(), false))
                .hasMessageContaining("own account");
    }

    private AssetForm sampleAsset(String code) {
        AssetForm f = new AssetForm();
        f.setAssetCode(code);
        f.setName("Test asset");
        f.setAssetType(AssetType.SERVER);
        f.setDepartmentId(departmentRepository.findAll().getFirst().getId());
        f.setLocationId(locationRepository.findAll().getFirst().getId());
        f.setCriticality(Criticality.LOW);
        f.setEnvironment(Environment.TEST);
        return f;
    }

    private Long taskIdByCode(String code) {
        return taskRepository.findAll().stream().filter(t -> t.getTaskCode().equals(code)).findFirst().orElseThrow().getId();
    }

    private long countAudit(AuditAction action) {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action = ?", Long.class, action.name());
        return n == null ? 0 : n;
    }
}

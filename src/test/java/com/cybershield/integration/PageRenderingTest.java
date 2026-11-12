package com.cybershield.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cybershield.repository.AuditPlanRepository;
import com.cybershield.repository.RiskAssessmentRepository;
import com.cybershield.repository.SoftwareRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Renders every Thymeleaf page for the roles allowed to see it (so template errors fail the build),
 * and drives the SRV-104 workflow through the real HTTP endpoints, including CSRF-protected forms and the upload.
 */
class PageRenderingTest extends IntegrationTestBase {

    @Autowired
    WebApplicationContext webContext;
    @Autowired
    TestData data;
    @Autowired
    SoftwareRepository softwareRepository;
    @Autowired
    RiskAssessmentRepository riskRepository;
    @Autowired
    AuditPlanRepository planRepository;

    MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    private void expectOk(String username, List<String> urls) throws Exception {
        for (String url : urls) {
            mvc.perform(get(url).with(user(principal(username))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("CyberShield")));
        }
    }

    @Test
    void loginAndErrorPagesRender() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));
        mvc.perform(get("/assets/999999").with(user(principal("manager")))).andExpect(status().isNotFound());
        mvc.perform(get("/users").with(user(principal("engineer")))).andExpect(status().isForbidden());
    }

    @Test
    void managerCanOpenEveryPage() throws Exception {
        Long softwareId = softwareRepository.findAll().getFirst().getId();
        Long riskId = riskRepository.findAll().getFirst().getId();
        Long planId = planRepository.findAll().getFirst().getId();
        expectOk("manager", List.of("/dashboard", "/assets", "/assets?q=SRV&sort=name&dir=desc&page=0",
                "/assets/" + data.assetId("SRV-104"), "/assets/new", "/assets/" + data.assetId("SRV-104") + "/edit",
                "/software", "/software/" + softwareId, "/software/new", "/software/" + softwareId + "/edit",
                "/vulnerabilities", "/vulnerabilities?severity=CRITICAL", "/vulnerabilities/" + data.vulnerabilityId("VUL-0001"),
                "/vulnerabilities/new", "/vulnerabilities/" + data.vulnerabilityId("VUL-0001") + "/edit",
                "/asset-vulnerabilities/" + data.instance("SRV-104", "VUL-0001").getId(),
                "/remediation", "/remediation?status=PENDING_VERIFICATION", "/remediation?overdue=true",
                "/remediation/" + data.taskId("REM-0003"), "/remediation/" + data.taskId("REM-0004"),
                "/remediation/" + data.taskId("REM-0006"),
                "/alerts", "/alerts/" + data.alertId("ALT-0001"), "/alerts/new",
                "/incidents", "/incidents/" + data.incidentId("INC-0001"), "/incidents/new",
                "/incidents/new?alertId=" + data.alertId("ALT-0006"), "/incidents/" + data.incidentId("INC-0001") + "/edit",
                "/risks", "/risks/" + riskId, "/risks/new", "/risks/" + riskId + "/edit",
                "/risks/new?assetVulnerabilityId=" + data.instance("SRV-104", "VUL-0001").getId(),
                "/audits", "/audits/" + planId, "/audits/new", "/audits/" + planId + "/edit", "/audits/frameworks",
                "/audits/controls", "/audits/findings", "/audits/findings/" + data.findingId("FND-0001"),
                "/audits/actions", "/audits/actions/" + data.firstActionOfFinding("FND-0002"),
                "/notifications", "/notifications?unread=true", "/users", "/users/new",
                "/users/" + data.userId("analyst"), "/users/" + data.userId("analyst") + "/edit",
                "/settings", "/profile", "/reports", "/search?q=SRV", "/system", "/system/audit-logs"));
    }

    @Test
    void otherRolesCanOpenTheirPages() throws Exception {
        expectOk("analyst", List.of("/dashboard", "/assets", "/vulnerabilities", "/remediation", "/alerts", "/incidents",
                "/risks", "/reports", "/profile", "/audits/actions", "/audits/actions/" + data.firstActionOfFinding("FND-0002"),
                "/remediation/" + data.taskId("REM-0003"), "/asset-vulnerabilities/" + data.instance("SRV-104", "VUL-0001").getId()));
        expectOk("engineer", List.of("/dashboard", "/assets", "/assets/" + data.assetId("SRV-104"), "/vulnerabilities",
                "/vulnerabilities/" + data.vulnerabilityId("VUL-0001"), "/remediation", "/remediation/" + data.taskId("REM-0001"),
                "/audits/actions", "/audits/actions/" + data.firstActionOfFinding("FND-0001"), "/notifications", "/reports",
                "/profile", "/search?q=REM"));
        expectOk("auditor", List.of("/dashboard", "/assets", "/assets/" + data.assetId("SRV-104"), "/software", "/risks",
                "/audits", "/audits/frameworks", "/audits/controls", "/audits/findings",
                "/audits/findings/" + data.findingId("FND-0002"), "/audits/actions",
                "/audits/actions/" + data.firstActionOfFinding("FND-0002"), "/reports", "/profile"));
        // engineers cannot open tasks of other engineers
        mvc.perform(get("/remediation/" + data.taskId("REM-0004")).with(user(principal("engineer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restEndpointsReturnJson() throws Exception {
        mvc.perform(get("/api/dashboard/charts").with(user(principal("manager"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assetDistribution.labels").isArray());
        mvc.perform(get("/api/notifications/summary").with(user(principal("engineer"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").isNumber());
        mvc.perform(get("/api/search").param("q", "SRV-104").with(user(principal("analyst"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].identifier").value("SRV-104"));
        mvc.perform(post("/api/notifications/read-all").with(user(principal("engineer"))))
                .andExpect(status().isForbidden()); // CSRF token missing
        mvc.perform(post("/api/notifications/read-all").with(user(principal("engineer"))).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void reportsDownloadOverHttp() throws Exception {
        mvc.perform(get("/reports/assets.pdf").with(user(principal("manager"))))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"));
        mvc.perform(get("/reports/remediation.csv").param("taskStatus", "ASSIGNED").with(user(principal("engineer"))))
                .andExpect(status().isOk());
        mvc.perform(get("/reports/incidents.pdf").with(user(principal("engineer")))).andExpect(status().isForbidden());
    }

    @Test
    void srv104WorkflowThroughHttpEndpoints() throws Exception {
        Long avId = data.instance("SRV-104", "VUL-0001").getId();
        String location = mvc.perform(post("/remediation").with(user(principal("analyst"))).with(csrf())
                        .param("assetVulnerabilityId", avId.toString())
                        .param("title", "Patch SRV-104").param("description", "Upgrade the framework")
                        .param("priority", "CRITICAL").param("engineerId", data.userId("engineer").toString())
                        .param("dueDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/remediation/*"))
                .andReturn().getResponse().getRedirectedUrl();
        String taskUrl = location;
        mvc.perform(post(taskUrl + "/start").with(user(principal("engineer"))).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(taskUrl + "/notes").param("note", "Working on it").with(user(principal("engineer"))).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart(taskUrl + "/evidence")
                        .file(new MockMultipartFile("file", "proof.txt", "text/plain", "done".getBytes(StandardCharsets.UTF_8)))
                        .param("description", "proof").with(user(principal("engineer"))).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(taskUrl + "/submit").with(user(principal("engineer"))).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get(taskUrl).with(user(principal("manager")))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Review submitted evidence")));
        mvc.perform(post(taskUrl + "/review").param("decision", "APPROVED").param("comments", "ok")
                .with(user(principal("manager"))).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get(taskUrl).with(user(principal("manager")))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Evidence Approved")));
    }
}

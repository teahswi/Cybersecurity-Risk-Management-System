package com.cybershield.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cybershield.dto.SearchResult;
import com.cybershield.report.ReportFilter;
import com.cybershield.report.ReportType;
import com.cybershield.service.DashboardService;
import com.cybershield.service.ReportService;
import com.cybershield.service.SearchService;
import com.cybershield.service.SystemService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/** Dashboard statistics, role-filtered global search, CSV/PDF reports and the system health check. */
class ReportDashboardSearchTest extends IntegrationTestBase {

    @Autowired
    ReportService reportService;
    @Autowired
    DashboardService dashboardService;
    @Autowired
    SearchService searchService;
    @Autowired
    SystemService systemService;

    @Test
    void everyReportGeneratesRealPdfAndCsvForTheManager() {
        loginAs("manager");
        for (ReportType type : ReportType.values()) {
            byte[] pdf = reportService.pdf(type, new ReportFilter());
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).as(type + " PDF").isEqualTo("%PDF-");
            String csv = new String(reportService.csv(type, new ReportFilter()), StandardCharsets.UTF_8);
            assertThat(csv).contains(type.getTitle());
            assertThat(reportService.generate(type, new ReportFilter()).rows()).as(type + " has database rows").isNotEmpty();
        }
        String assets = new String(reportService.csv(ReportType.ASSETS, new ReportFilter()), StandardCharsets.UTF_8);
        assertThat(assets).contains("SRV-104").contains("Finance Production Server");
    }

    @Test
    void reportsAreRestrictedByRole() {
        loginAs("engineer");
        assertThatThrownBy(() -> reportService.generate(ReportType.INCIDENTS, new ReportFilter()))
                .isInstanceOf(AccessDeniedException.class);
        var own = reportService.generate(ReportType.REMEDIATION, new ReportFilter());
        assertThat(own.rows()).allMatch(r -> r.get(7).equals("Vikram Singh"));
        loginAs("auditor");
        assertThatThrownBy(() -> reportService.generate(ReportType.VULNERABILITIES, new ReportFilter()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(reportService.generate(ReportType.AUDIT, new ReportFilter()).rows()).isNotEmpty();
    }

    @Test
    void dashboardIsCalculatedFromDatabaseForEachRole() {
        for (String username : new String[]{"analyst", "engineer", "auditor", "manager"}) {
            loginAs(username);
            var dashboard = dashboardService.build();
            assertThat(dashboard.cards()).as(username + " cards").isNotEmpty();
            assertThat(dashboardService.charts()).as(username + " charts").isNotEmpty();
        }
        loginAs("manager");
        var manager = dashboardService.build();
        assertThat(manager.cards()).extracting(c -> c.label()).contains("Total assets", "Critical vulnerabilities",
                "Open incidents", "Active security alerts", "Pending remediation tasks", "Overdue tasks", "Open audit findings");
        assertThat(manager.databaseUp()).isTrue();
        assertThat(dashboardService.charts()).containsKeys("vulnerabilitySeverity", "incidentStatus", "assetDistribution",
                "remediationProgress", "riskDistribution");
        loginAs("engineer");
        assertThat(dashboardService.charts()).containsOnlyKeys("remediationProgress");
    }

    @Test
    void globalSearchIsFilteredByRole() {
        loginAs("analyst");
        assertThat(searchService.search("SRV-104")).extracting(SearchResult::category).contains("Asset");
        assertThat(searchService.search("INC-0001")).extracting(SearchResult::category).contains("Incident");
        assertThat(searchService.search("a")).as("minimum 2 characters").isEmpty();

        loginAs("auditor");
        assertThat(searchService.search("INC-0001")).as("auditors cannot see incidents").isEmpty();
        loginAs("engineer2");
        assertThat(searchService.search("REM-0001")).as("engineer2 is not assigned REM-0001").isEmpty();
        assertThat(searchService.search("REM-0004")).extracting(SearchResult::identifier).contains("REM-0004");
    }

    @Test
    void systemHealthUsesRealQuery() {
        loginAs("manager");
        var health = systemService.health();
        assertThat(health.databaseUp()).isTrue();
        assertThat(health.registeredUsers()).isGreaterThanOrEqualTo(6);
        assertThat(health.schemaVersion()).isEqualTo("10");
        loginAs("analyst");
        assertThatThrownBy(() -> systemService.health()).isInstanceOf(AccessDeniedException.class);
    }
}

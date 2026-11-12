package com.cybershield.controller.api;

import com.cybershield.dto.ChartData;
import com.cybershield.service.DashboardService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chart.js data, calculated from MySQL on every request. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/charts")
    public Map<String, ChartData> charts() {
        return dashboardService.charts();
    }
}

package com.cybershield.controller;

import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.Severity;
import com.cybershield.enums.TaskStatus;
import com.cybershield.enums.VulnerabilityStatus;
import com.cybershield.report.ReportFilter;
import com.cybershield.report.ReportType;
import com.cybershield.repository.AuditPlanRepository;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.service.ReportService;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** Reports page with filters, plus CSV and PDF downloads generated from MySQL. */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReferenceDataService referenceData;
    private final AuditPlanRepository planRepository;

    public ReportController(ReportService reportService, ReferenceDataService referenceData,
                            AuditPlanRepository planRepository) {
        this.reportService = reportService;
        this.referenceData = referenceData;
        this.planRepository = planRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        model.addAttribute("reports", reportService.availableReports());
        model.addAttribute("departments", referenceData.departments());
        model.addAttribute("assetStatuses", AssetStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("vulnerabilityStatuses", VulnerabilityStatus.values());
        model.addAttribute("incidentStatuses", IncidentStatus.values());
        model.addAttribute("taskStatuses", TaskStatus.values());
        model.addAttribute("riskLevels", RiskLevel.values());
        model.addAttribute("riskStatuses", RiskStatus.values());
        model.addAttribute("plans", planRepository.findAllForSelection().stream()
                .map(p -> new com.cybershield.dto.Option(p.getId(), p.getPlanCode() + " - " + p.getName())).toList());
        return "reports/index";
    }

    @GetMapping("/{type}.csv")
    public ResponseEntity<byte[]> csv(@PathVariable String type, @ModelAttribute ReportFilter filter) {
        ReportType reportType = parse(type);
        return download(reportService.csv(reportType, filter), reportType, "csv", new MediaType("text", "csv"));
    }

    @GetMapping("/{type}.pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String type, @ModelAttribute ReportFilter filter) {
        ReportType reportType = parse(type);
        return download(reportService.pdf(reportType, filter), reportType, "pdf", MediaType.APPLICATION_PDF);
    }

    private static ReportType parse(String type) {
        try {
            return ReportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.cybershield.exception.ResourceNotFoundException("Unknown report type " + type + ".");
        }
    }

    private static ResponseEntity<byte[]> download(byte[] body, ReportType type, String ext, MediaType mediaType) {
        String filename = "cybershield-" + type.getSlug() + "-report-" + LocalDate.now() + "." + ext;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}

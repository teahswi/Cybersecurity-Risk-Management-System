package com.cybershield.controller;

import com.cybershield.enums.AuditAction;
import com.cybershield.enums.AuditModule;
import com.cybershield.service.AuditLogService;
import com.cybershield.service.SystemService;
import com.cybershield.util.Paging;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** System overview and the read-only audit log browser (Security Manager). */
@Controller
@RequestMapping("/system")
public class SystemController {

    private final SystemService systemService;
    private final AuditLogService auditLogService;

    public SystemController(SystemService systemService, AuditLogService auditLogService) {
        this.systemService = systemService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("health", systemService.health());
        model.addAttribute("recent", auditLogService.recent(10));
        return "system/index";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(required = false) String q, @RequestParam(required = false) AuditAction action,
                            @RequestParam(required = false) AuditModule module,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", auditLogService.search(q, action, module, null, from, to,
                Paging.of(page, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))));
        model.addAttribute("q", q);
        model.addAttribute("action", action);
        model.addAttribute("module", module);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("modules", AuditModule.values());
        return "system/audit-logs";
    }
}

package com.cybershield.controller;

import com.cybershield.dto.AlertForm;
import com.cybershield.enums.AlertSource;
import com.cybershield.enums.AlertStatus;
import com.cybershield.enums.Severity;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.AlertService;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.util.Paging;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alerts")
public class AlertController {

    private static final Set<String> SORTS = Set.of("alertCode", "title", "severity", "status", "detectedAt");

    private final AlertService alertService;
    private final ReferenceDataService referenceData;

    public AlertController(AlertService alertService, ReferenceDataService referenceData) {
        this.alertService = alertService;
        this.referenceData = referenceData;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) AlertStatus status,
                       @RequestParam(required = false) Severity severity, @RequestParam(required = false) AlertSource source,
                       @RequestParam(defaultValue = "false") boolean mine, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "detectedAt") String sort, @RequestParam(defaultValue = "desc") String dir,
                       Model model) {
        model.addAttribute("page", alertService.search(q, status, severity, source, mine,
                Paging.of(page, sort, dir, SORTS, "detectedAt")));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("severity", severity);
        model.addAttribute("source", source);
        model.addAttribute("mine", mine);
        model.addAttribute("statuses", AlertStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("sources", AlertSource.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "alerts/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("a", alertService.detail(id));
        model.addAttribute("analysts", referenceData.analystsAndManagers());
        return "alerts/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AlertForm());
        return formView(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AlertForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model);
        }
        try {
            Long id = alertService.create(form);
            redirect.addFlashAttribute("success", "Alert recorded.");
            return "redirect:/alerts/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model);
        }
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long analystId,
                         RedirectAttributes redirect) {
        alertService.assign(id, analystId);
        redirect.addFlashAttribute("success", "Alert assigned and the analyst notified.");
        return "redirect:/alerts/" + id;
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam AlertStatus status, RedirectAttributes redirect) {
        alertService.changeStatus(id, status);
        redirect.addFlashAttribute("success", "Alert status changed to " + status.getLabel() + ".");
        return "redirect:/alerts/" + id;
    }

    private String formView(Model model) {
        model.addAttribute("sources", AlertSource.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("assets", referenceData.activeAssets());
        model.addAttribute("analysts", referenceData.analystsAndManagers());
        return "alerts/form";
    }
}

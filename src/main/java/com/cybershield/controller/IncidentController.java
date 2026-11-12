package com.cybershield.controller;

import com.cybershield.dto.IncidentForm;
import com.cybershield.enums.IncidentStatus;
import com.cybershield.enums.IncidentType;
import com.cybershield.enums.Severity;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.IncidentService;
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
@RequestMapping("/incidents")
public class IncidentController {

    private static final Set<String> SORTS = Set.of("incidentCode", "title", "severity", "status", "detectedAt");

    private final IncidentService incidentService;
    private final ReferenceDataService referenceData;

    public IncidentController(IncidentService incidentService, ReferenceDataService referenceData) {
        this.incidentService = incidentService;
        this.referenceData = referenceData;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) IncidentStatus status,
                       @RequestParam(required = false) Severity severity, @RequestParam(required = false) IncidentType type,
                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "detectedAt") String sort,
                       @RequestParam(defaultValue = "desc") String dir, Model model) {
        model.addAttribute("page", incidentService.search(q, status, severity, type,
                Paging.of(page, sort, dir, SORTS, "detectedAt")));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("severity", severity);
        model.addAttribute("type", type);
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("types", IncidentType.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "incidents/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("i", incidentService.detail(id));
        model.addAttribute("analysts", referenceData.analystsAndManagers());
        model.addAttribute("alertOptions", incidentService.linkableAlerts());
        model.addAttribute("assetOptions", referenceData.activeAssets());
        return "incidents/detail";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long alertId, Model model) {
        IncidentForm form = new IncidentForm();
        form.setAlertId(alertId);
        model.addAttribute("form", form);
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") IncidentForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            Long id = incidentService.create(form);
            redirect.addFlashAttribute("success", "Incident created.");
            return "redirect:/incidents/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", incidentService.formFor(id));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") IncidentForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            incidentService.update(id, form);
            redirect.addFlashAttribute("success", "Incident updated.");
            return "redirect:/incidents/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, id);
        }
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long analystId,
                         RedirectAttributes redirect) {
        incidentService.assign(id, analystId);
        redirect.addFlashAttribute("success", "Investigator assigned and notified.");
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam IncidentStatus status,
                         @RequestParam(required = false) String resolutionDetails, RedirectAttributes redirect) {
        incidentService.changeStatus(id, status, resolutionDetails);
        redirect.addFlashAttribute("success", "Incident status changed to " + status.getLabel() + ".");
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/alerts")
    public String linkAlert(@PathVariable Long id, @RequestParam(required = false) Long alertId,
                            RedirectAttributes redirect) {
        incidentService.linkAlert(id, alertId);
        redirect.addFlashAttribute("success", "Alert linked to the incident.");
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/assets")
    public String linkAsset(@PathVariable Long id, @RequestParam(required = false) Long assetId,
                            @RequestParam(required = false) String impact, RedirectAttributes redirect) {
        incidentService.linkAsset(id, assetId, impact);
        redirect.addFlashAttribute("success", "Affected asset linked.");
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String note, RedirectAttributes redirect) {
        incidentService.addNote(id, note);
        redirect.addFlashAttribute("success", "Investigation note added.");
        return "redirect:/incidents/" + id + "#timeline";
    }

    private String formView(Model model, Long id) {
        model.addAttribute("incidentId", id);
        model.addAttribute("types", IncidentType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("analysts", referenceData.analystsAndManagers());
        return "incidents/form";
    }
}

package com.cybershield.controller;

import com.cybershield.dto.RiskForm;
import com.cybershield.dto.TreatmentForm;
import com.cybershield.enums.RiskLevel;
import com.cybershield.enums.RiskStatus;
import com.cybershield.enums.TreatmentStatus;
import com.cybershield.enums.TreatmentStrategy;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.service.RiskService;
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
@RequestMapping("/risks")
public class RiskController {

    private static final Set<String> SORTS = Set.of("riskCode", "title", "riskScore", "status", "updatedAt");

    private final RiskService riskService;
    private final ReferenceDataService referenceData;

    public RiskController(RiskService riskService, ReferenceDataService referenceData) {
        this.riskService = riskService;
        this.referenceData = referenceData;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) RiskLevel level,
                       @RequestParam(required = false) RiskStatus status, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "riskScore") String sort, @RequestParam(defaultValue = "desc") String dir,
                       Model model) {
        model.addAttribute("page", riskService.search(q, level, status, Paging.of(page, sort, dir, SORTS, "riskScore")));
        model.addAttribute("matrix", riskService.matrix());
        model.addAttribute("q", q);
        model.addAttribute("level", level);
        model.addAttribute("status", status);
        model.addAttribute("levels", RiskLevel.values());
        model.addAttribute("statuses", RiskStatus.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "risks/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("r", riskService.detail(id));
        model.addAttribute("treatmentForm", new TreatmentForm());
        model.addAttribute("strategies", TreatmentStrategy.values());
        model.addAttribute("treatmentStatuses", TreatmentStatus.values());
        model.addAttribute("users", referenceData.activeUsers());
        return "risks/detail";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long assetId,
                          @RequestParam(required = false) Long assetVulnerabilityId, Model model) {
        model.addAttribute("form", riskService.newForm(assetId, assetVulnerabilityId));
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") RiskForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            Long id = riskService.create(form);
            redirect.addFlashAttribute("success", "Risk assessment saved. The score was calculated by the server.");
            return "redirect:/risks/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", riskService.formFor(id));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RiskForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            riskService.update(id, form);
            redirect.addFlashAttribute("success", "Risk assessment updated.");
            return "redirect:/risks/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, id);
        }
    }

    @PostMapping("/{id}/treatments")
    public String addTreatment(@PathVariable Long id, @Valid @ModelAttribute("treatmentForm") TreatmentForm form,
                               BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/risks/" + id;
        }
        riskService.addTreatment(id, form);
        redirect.addFlashAttribute("success", "Treatment added.");
        return "redirect:/risks/" + id;
    }

    @PostMapping("/treatments/{treatmentId}/status")
    public String treatmentStatus(@PathVariable Long treatmentId, @RequestParam TreatmentStatus status,
                                  RedirectAttributes redirect) {
        Long riskId = riskService.updateTreatmentStatus(treatmentId, status);
        redirect.addFlashAttribute("success", "Treatment status updated.");
        return "redirect:/risks/" + riskId;
    }

    private String formView(Model model, Long id) {
        model.addAttribute("riskId", id);
        model.addAttribute("assets", referenceData.activeAssets());
        model.addAttribute("users", referenceData.activeUsers());
        model.addAttribute("strategies", TreatmentStrategy.values());
        model.addAttribute("statuses", RiskStatus.values());
        return "risks/form";
    }
}

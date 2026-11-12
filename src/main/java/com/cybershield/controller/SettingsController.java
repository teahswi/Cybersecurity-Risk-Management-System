package com.cybershield.controller;

import com.cybershield.dto.ReferenceForm;
import com.cybershield.service.ReferenceAdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Departments and office locations (Security Manager). */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final ReferenceAdminService referenceAdminService;

    public SettingsController(ReferenceAdminService referenceAdminService) {
        this.referenceAdminService = referenceAdminService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("departments", referenceAdminService.departments());
        model.addAttribute("locations", referenceAdminService.locations());
        if (!model.containsAttribute("departmentForm")) {
            model.addAttribute("departmentForm", new ReferenceForm());
        }
        if (!model.containsAttribute("locationForm")) {
            model.addAttribute("locationForm", new ReferenceForm());
        }
        return "settings/index";
    }

    @PostMapping("/departments")
    public String saveDepartment(@RequestParam(required = false) Long id,
                                 @Valid @ModelAttribute("departmentForm") ReferenceForm form, BindingResult result,
                                 RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/settings";
        }
        referenceAdminService.saveDepartment(id, form);
        redirect.addFlashAttribute("success", "Department saved.");
        return "redirect:/settings";
    }

    @PostMapping("/locations")
    public String saveLocation(@RequestParam(required = false) Long id,
                               @Valid @ModelAttribute("locationForm") ReferenceForm form, BindingResult result,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/settings";
        }
        referenceAdminService.saveLocation(id, form);
        redirect.addFlashAttribute("success", "Office location saved.");
        return "redirect:/settings";
    }
}

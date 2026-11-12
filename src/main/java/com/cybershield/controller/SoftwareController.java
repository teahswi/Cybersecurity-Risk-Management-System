package com.cybershield.controller;

import com.cybershield.dto.InstallSoftwareForm;
import com.cybershield.dto.SoftwareForm;
import com.cybershield.enums.LicenseType;
import com.cybershield.enums.SoftwareCategory;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.service.SoftwareService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SoftwareController {

    private static final Set<String> SORTS = Set.of("name", "vendor", "category");

    private final SoftwareService softwareService;
    private final ReferenceDataService referenceData;

    public SoftwareController(SoftwareService softwareService, ReferenceDataService referenceData) {
        this.softwareService = softwareService;
        this.referenceData = referenceData;
    }

    @GetMapping("/software")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) SoftwareCategory category,
                       @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "name") String sort,
                       @RequestParam(defaultValue = "asc") String dir, Model model) {
        model.addAttribute("page", softwareService.search(q, category, Paging.of(page, sort, dir, SORTS, "name")));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("categories", SoftwareCategory.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "software/list";
    }

    @GetMapping("/software/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("s", softwareService.detail(id));
        InstallSoftwareForm install = new InstallSoftwareForm();
        install.setSoftwareId(id);
        model.addAttribute("installForm", install);
        model.addAttribute("assetOptions", referenceData.activeAssets());
        model.addAttribute("licenseTypes", LicenseType.values());
        return "software/detail";
    }

    @GetMapping("/software/new")
    public String newForm(Model model) {
        model.addAttribute("form", new SoftwareForm());
        return formView(model, null);
    }

    @PostMapping("/software")
    public String create(@Valid @ModelAttribute("form") SoftwareForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            Long id = softwareService.create(form);
            redirect.addFlashAttribute("success", "Software added to the catalogue.");
            return "redirect:/software/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, null);
        }
    }

    @GetMapping("/software/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", softwareService.formFor(id));
        return formView(model, id);
    }

    @PostMapping("/software/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SoftwareForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            softwareService.update(id, form);
            redirect.addFlashAttribute("success", "Software updated.");
            return "redirect:/software/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, id);
        }
    }

    /** Links software to an asset. Used from both the asset page and the software page. */
    @PostMapping("/asset-software")
    public String install(@Valid @ModelAttribute("installForm") InstallSoftwareForm form, BindingResult result,
                          @RequestParam(defaultValue = "asset") String returnTo, RedirectAttributes redirect) {
        String back = "software".equals(returnTo) && form.getSoftwareId() != null
                ? "redirect:/software/" + form.getSoftwareId()
                : "redirect:/assets/" + form.getAssetId();
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return back;
        }
        softwareService.install(form);
        redirect.addFlashAttribute("success", "Software installation recorded.");
        return back;
    }

    @PostMapping("/asset-software/{id}/delete")
    public String uninstall(@PathVariable Long id, RedirectAttributes redirect) {
        Long assetId = softwareService.uninstall(id);
        redirect.addFlashAttribute("success", "Software installation removed.");
        return "redirect:/assets/" + assetId;
    }

    private String formView(Model model, Long id) {
        model.addAttribute("softwareId", id);
        model.addAttribute("categories", SoftwareCategory.values());
        return "software/form";
    }
}

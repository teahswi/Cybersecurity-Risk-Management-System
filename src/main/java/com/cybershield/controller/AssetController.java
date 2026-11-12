package com.cybershield.controller;

import com.cybershield.dto.AssetFilter;
import com.cybershield.dto.AssetForm;
import com.cybershield.dto.InstallSoftwareForm;
import com.cybershield.dto.LinkVulnerabilityForm;
import com.cybershield.enums.AssetStatus;
import com.cybershield.enums.AssetType;
import com.cybershield.enums.Criticality;
import com.cybershield.enums.Environment;
import com.cybershield.enums.LicenseType;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.security.CurrentUserService;
import com.cybershield.service.AssetService;
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

/** Asset inventory pages. Controllers only translate HTTP <-> service calls; rules live in AssetService. */
@Controller
@RequestMapping("/assets")
public class AssetController {

    private static final Set<String> SORTS = Set.of("assetCode", "name", "assetType", "criticality", "environment",
            "status", "updatedAt");

    private final AssetService assetService;
    private final ReferenceDataService referenceData;
    private final CurrentUserService currentUserService;

    public AssetController(AssetService assetService, ReferenceDataService referenceData,
                           CurrentUserService currentUserService) {
        this.assetService = assetService;
        this.referenceData = referenceData;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(@ModelAttribute("filter") AssetFilter filter,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "assetCode") String sort,
                       @RequestParam(defaultValue = "asc") String dir, Model model) {
        model.addAttribute("page", assetService.search(filter, Paging.of(page, sort, dir, SORTS, "assetCode")));
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        addOptions(model);
        return "assets/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("p", assetService.detailPage(id));
        if (currentUserService.isManager()) {
            InstallSoftwareForm install = new InstallSoftwareForm();
            install.setAssetId(id);
            model.addAttribute("installForm", install);
            model.addAttribute("softwareOptions", referenceData.software());
            model.addAttribute("licenseTypes", LicenseType.values());
        }
        if (currentUserService.isAnalyst() || currentUserService.isManager()) {
            LinkVulnerabilityForm link = new LinkVulnerabilityForm();
            link.setAssetId(id);
            model.addAttribute("linkForm", link);
            model.addAttribute("vulnerabilityOptions", referenceData.vulnerabilities());
        }
        return "assets/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new AssetForm());
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AssetForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            Long id = assetService.create(form);
            redirect.addFlashAttribute("success", "Asset " + form.getAssetCode().toUpperCase() + " was created.");
            return "redirect:/assets/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", assetService.formFor(id));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AssetForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            assetService.update(id, form);
            redirect.addFlashAttribute("success", "Asset updated.");
            return "redirect:/assets/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, id);
        }
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes redirect) {
        assetService.archive(id);
        redirect.addFlashAttribute("success", "Asset archived. Its history is preserved.");
        return "redirect:/assets/" + id;
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirect) {
        assetService.restore(id);
        redirect.addFlashAttribute("success", "Asset restored to Active.");
        return "redirect:/assets/" + id;
    }

    private String formView(Model model, Long id) {
        model.addAttribute("assetId", id);
        model.addAttribute("departments", referenceData.departments());
        model.addAttribute("locations", referenceData.locations());
        model.addAttribute("owners", referenceData.activeUsers());
        addOptions(model);
        return "assets/form";
    }

    private void addOptions(Model model) {
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("criticalities", Criticality.values());
        model.addAttribute("environments", Environment.values());
        model.addAttribute("statuses", AssetStatus.values());
        if (!model.containsAttribute("departments")) {
            model.addAttribute("departments", referenceData.departments());
            model.addAttribute("locations", referenceData.locations());
        }
    }
}

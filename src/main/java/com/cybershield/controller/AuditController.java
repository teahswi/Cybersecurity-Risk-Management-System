package com.cybershield.controller;

import com.cybershield.dto.AuditPlanForm;
import com.cybershield.dto.ControlForm;
import com.cybershield.dto.CorrectiveActionForm;
import com.cybershield.dto.FindingForm;
import com.cybershield.dto.AuditViews;
import com.cybershield.enums.AuditPlanStatus;
import com.cybershield.enums.AuditReviewDecision;
import com.cybershield.enums.CorrectiveActionStatus;
import com.cybershield.enums.FindingStatus;
import com.cybershield.enums.Severity;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.AuditService;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.util.Paging;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Audit plans, frameworks, controls, findings, corrective actions and audit evidence. */
@Controller
@RequestMapping("/audits")
public class AuditController {

    private static final Set<String> PLAN_SORTS = Set.of("planCode", "name", "startDate", "targetCompletionDate", "status");

    private final AuditService auditService;
    private final ReferenceDataService referenceData;

    public AuditController(AuditService auditService, ReferenceDataService referenceData) {
        this.auditService = auditService;
        this.referenceData = referenceData;
    }

    // ---------------------------------------------------------------- plans

    @GetMapping
    public String plans(@RequestParam(required = false) String q, @RequestParam(required = false) AuditPlanStatus status,
                        @RequestParam(required = false) Long frameworkId, @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "startDate") String sort, @RequestParam(defaultValue = "desc") String dir,
                        Model model) {
        model.addAttribute("page", auditService.searchPlans(q, status, frameworkId,
                Paging.of(page, sort, dir, PLAN_SORTS, "startDate")));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("frameworkId", frameworkId);
        model.addAttribute("statuses", AuditPlanStatus.values());
        model.addAttribute("frameworks", referenceData.frameworks());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "audits/plans";
    }

    @GetMapping("/{id:\\d+}")
    public String planDetail(@PathVariable Long id, Model model) {
        AuditViews.PlanDetail plan = auditService.planDetail(id);
        model.addAttribute("plan", plan);
        model.addAttribute("findingForm", new FindingForm());
        model.addAttribute("controls", referenceData.controls(plan.frameworkId()));
        model.addAttribute("severities", Severity.values());
        return "audits/plan-detail";
    }

    @GetMapping("/new")
    public String newPlan(Model model) {
        model.addAttribute("form", new AuditPlanForm());
        return planForm(model, null);
    }

    @PostMapping
    public String createPlan(@Valid @ModelAttribute("form") AuditPlanForm form, BindingResult result, Model model,
                             RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return planForm(model, null);
        }
        try {
            Long id = auditService.createPlan(form);
            redirect.addFlashAttribute("success", "Audit plan created.");
            return "redirect:/audits/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return planForm(model, null);
        }
    }

    @GetMapping("/{id:\\d+}/edit")
    public String editPlan(@PathVariable Long id, Model model) {
        model.addAttribute("form", auditService.planFormFor(id));
        return planForm(model, id);
    }

    @PostMapping("/{id:\\d+}")
    public String updatePlan(@PathVariable Long id, @Valid @ModelAttribute("form") AuditPlanForm form,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return planForm(model, id);
        }
        try {
            auditService.updatePlan(id, form);
            redirect.addFlashAttribute("success", "Audit plan updated.");
            return "redirect:/audits/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return planForm(model, id);
        }
    }

    @PostMapping("/{id:\\d+}/status")
    public String planStatus(@PathVariable Long id, @RequestParam AuditPlanStatus status, RedirectAttributes redirect) {
        auditService.changePlanStatus(id, status);
        redirect.addFlashAttribute("success", "Audit plan status changed to " + status.getLabel() + ".");
        return "redirect:/audits/" + id;
    }

    @PostMapping("/{id:\\d+}/findings")
    public String recordFinding(@PathVariable Long id, @Valid @ModelAttribute("findingForm") FindingForm form,
                                BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/audits/" + id;
        }
        Long findingId = auditService.recordFinding(id, form);
        redirect.addFlashAttribute("success", "Finding recorded. Add corrective actions below.");
        return "redirect:/audits/findings/" + findingId;
    }

    // ---------------------------------------------------------------- frameworks & controls

    @GetMapping("/frameworks")
    public String frameworks(Model model) {
        model.addAttribute("frameworks", auditService.frameworks());
        return "audits/frameworks";
    }

    @GetMapping("/controls")
    public String controls(@RequestParam(required = false) Long frameworkId, @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", auditService.controls(frameworkId, q, Paging.of(page, Sort.by("controlCode"))));
        model.addAttribute("frameworkId", frameworkId);
        model.addAttribute("q", q);
        model.addAttribute("frameworks", referenceData.frameworks());
        if (!model.containsAttribute("controlForm")) {
            ControlForm form = new ControlForm();
            form.setFrameworkId(frameworkId);
            model.addAttribute("controlForm", form);
        }
        return "audits/controls";
    }

    @PostMapping("/controls")
    public String addControl(@Valid @ModelAttribute("controlForm") ControlForm form, BindingResult result,
                             RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            redirect.addFlashAttribute("controlForm", form);
            return "redirect:/audits/controls";
        }
        auditService.addControl(form);
        redirect.addFlashAttribute("success", "Control " + form.getControlCode() + " added.");
        return "redirect:/audits/controls?frameworkId=" + form.getFrameworkId();
    }

    // ---------------------------------------------------------------- findings

    @GetMapping("/findings")
    public String findings(@RequestParam(required = false) String q, @RequestParam(required = false) FindingStatus status,
                           @RequestParam(required = false) Severity severity, @RequestParam(required = false) Long planId,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", auditService.searchFindings(q, status, severity, planId,
                Paging.of(page, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("severity", severity);
        model.addAttribute("planId", planId);
        model.addAttribute("statuses", FindingStatus.values());
        model.addAttribute("severities", Severity.values());
        return "audits/findings";
    }

    @GetMapping("/findings/{id}")
    public String findingDetail(@PathVariable Long id, Model model) {
        model.addAttribute("f", auditService.findingDetail(id));
        model.addAttribute("actionForm", new CorrectiveActionForm());
        model.addAttribute("users", referenceData.activeUsers());
        model.addAttribute("statuses", FindingStatus.values());
        return "audits/finding-detail";
    }

    @PostMapping("/findings/{id}/status")
    public String findingStatus(@PathVariable Long id, @RequestParam FindingStatus status, RedirectAttributes redirect) {
        auditService.changeFindingStatus(id, status);
        redirect.addFlashAttribute("success", "Finding status changed to " + status.getLabel() + ".");
        return "redirect:/audits/findings/" + id;
    }

    @PostMapping("/findings/{id}/actions")
    public String addAction(@PathVariable Long id, @Valid @ModelAttribute("actionForm") CorrectiveActionForm form,
                            BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/audits/findings/" + id;
        }
        auditService.addCorrectiveAction(id, form);
        redirect.addFlashAttribute("success", "Corrective action assigned and the owner notified.");
        return "redirect:/audits/findings/" + id;
    }

    // ---------------------------------------------------------------- corrective actions (any assigned user)

    @GetMapping("/actions")
    public String actions(@RequestParam(required = false) CorrectiveActionStatus status,
                          @RequestParam(defaultValue = "false") boolean mine, @RequestParam(defaultValue = "0") int page,
                          Model model) {
        model.addAttribute("page", auditService.searchActions(status, mine, Paging.of(page, Sort.by("deadline"))));
        model.addAttribute("status", status);
        model.addAttribute("mine", mine);
        model.addAttribute("statuses", CorrectiveActionStatus.values());
        return "audits/actions";
    }

    @GetMapping("/actions/{id}")
    public String actionDetail(@PathVariable Long id, Model model) {
        model.addAttribute("a", auditService.actionDetail(id));
        return "audits/action-detail";
    }

    @PostMapping("/actions/{id}/start")
    public String startAction(@PathVariable Long id, RedirectAttributes redirect) {
        auditService.startAction(id);
        redirect.addFlashAttribute("success", "Corrective action is now In Progress.");
        return "redirect:/audits/actions/" + id;
    }

    @PostMapping("/actions/{id}/evidence")
    public String uploadEvidence(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String description, RedirectAttributes redirect) {
        auditService.uploadEvidence(id, file, description);
        redirect.addFlashAttribute("success", "Audit evidence submitted. The auditor has been notified.");
        return "redirect:/audits/actions/" + id;
    }

    @PostMapping("/actions/{id}/review")
    public String reviewEvidence(@PathVariable Long id, @RequestParam(required = false) AuditReviewDecision decision,
                                 @RequestParam(required = false) String notes, RedirectAttributes redirect) {
        auditService.reviewEvidence(id, decision, notes);
        redirect.addFlashAttribute("success", decision == AuditReviewDecision.VERIFIED
                ? "Audit evidence verified." : "Audit evidence rejected; the action returned to In Progress.");
        return "redirect:/audits/actions/" + id;
    }

    @GetMapping("/actions/evidence/{evidenceId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long evidenceId) {
        return RemediationController.fileResponse(auditService.downloadEvidence(evidenceId));
    }

    private String planForm(Model model, Long id) {
        model.addAttribute("planId", id);
        model.addAttribute("frameworks", referenceData.frameworks());
        model.addAttribute("auditors", referenceData.auditors());
        model.addAttribute("statuses", AuditPlanStatus.values());
        return "audits/plan-form";
    }
}

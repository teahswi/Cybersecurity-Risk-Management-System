package com.cybershield.controller;

import com.cybershield.dto.TaskCreateForm;
import com.cybershield.enums.Priority;
import com.cybershield.enums.ReviewDecision;
import com.cybershield.enums.TaskStatus;
import com.cybershield.security.CurrentUserService;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.service.RemediationService;
import com.cybershield.util.Paging;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

/** Remediation workflow pages. Every POST maps to one transactional RemediationService method. */
@Controller
@RequestMapping("/remediation")
public class RemediationController {

    private static final Set<String> SORTS = Set.of("taskCode", "title", "priority", "status", "dueDate", "updatedAt");

    private final RemediationService remediationService;
    private final ReferenceDataService referenceData;
    private final CurrentUserService currentUserService;

    public RemediationController(RemediationService remediationService, ReferenceDataService referenceData,
                                 CurrentUserService currentUserService) {
        this.remediationService = remediationService;
        this.referenceData = referenceData;
        this.currentUserService = currentUserService;
    }

    /** Analysts/managers: all tasks. Engineers: "My Assigned Tasks" (filtered on the server). */
    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) TaskStatus status,
                       @RequestParam(required = false) Priority priority, @RequestParam(required = false) Long engineerId,
                       @RequestParam(defaultValue = "false") boolean overdue, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "dueDate") String sort, @RequestParam(defaultValue = "asc") String dir,
                       Model model) {
        model.addAttribute("page", remediationService.search(q, status, priority, engineerId, overdue,
                Paging.of(page, sort, dir, SORTS, "dueDate")));
        model.addAttribute("progress", remediationService.progress());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("engineerId", engineerId);
        model.addAttribute("overdue", overdue);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        if (currentUserService.isEngineer()) {
            model.addAttribute("myHistory", remediationService.myRecentHistory(10));
        } else {
            model.addAttribute("engineers", referenceData.engineers());
        }
        return "remediation/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("p", remediationService.taskPage(id));
        model.addAttribute("engineers", referenceData.engineers());
        model.addAttribute("today", LocalDate.now());
        return "remediation/detail";
    }

    /** Creates the task from the asset vulnerability page (steps 7-9 of the demo). */
    @PostMapping
    public String create(@Valid @ModelAttribute("taskForm") TaskCreateForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            redirect.addFlashAttribute("taskForm", form);
            return "redirect:/asset-vulnerabilities/" + form.getAssetVulnerabilityId();
        }
        Long id = remediationService.createTask(form);
        redirect.addFlashAttribute("success", form.getEngineerId() != null
                ? "Remediation task created and assigned. The engineer has been notified."
                : "Remediation task created. Assign an engineer to start the work.");
        return "redirect:/remediation/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long engineerId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                         RedirectAttributes redirect) {
        remediationService.assign(id, engineerId, dueDate);
        redirect.addFlashAttribute("success", "Engineer assigned and notified.");
        return "redirect:/remediation/" + id;
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes redirect) {
        remediationService.startWork(id);
        redirect.addFlashAttribute("success", "Task is now In Progress.");
        return "redirect:/remediation/" + id;
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id, @RequestParam String note, RedirectAttributes redirect) {
        remediationService.addNote(id, note);
        redirect.addFlashAttribute("success", "Note added to the task history.");
        return "redirect:/remediation/" + id + "#history";
    }

    @PostMapping("/{id}/evidence")
    public String uploadEvidence(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String description, RedirectAttributes redirect) {
        remediationService.uploadEvidence(id, file, description);
        redirect.addFlashAttribute("success", "Evidence uploaded. Submit it for verification when you are done.");
        return "redirect:/remediation/" + id + "#evidence";
    }

    @PostMapping("/{id}/evidence/{evidenceId}/delete")
    public String deleteEvidence(@PathVariable Long id, @PathVariable Long evidenceId, RedirectAttributes redirect) {
        remediationService.deleteDraftEvidence(id, evidenceId);
        redirect.addFlashAttribute("success", "Draft evidence removed.");
        return "redirect:/remediation/" + id + "#evidence";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, @RequestParam(required = false) String comment,
                         RedirectAttributes redirect) {
        remediationService.submitForVerification(id, comment);
        redirect.addFlashAttribute("success", "Evidence submitted. The task is now Pending Verification and reviewers were notified.");
        return "redirect:/remediation/" + id;
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id, @RequestParam(required = false) ReviewDecision decision,
                         @RequestParam(required = false) String comments, RedirectAttributes redirect) {
        remediationService.reviewEvidence(id, decision, comments);
        redirect.addFlashAttribute("success", decision == ReviewDecision.APPROVED
                ? "Evidence approved. The task is Completed and the vulnerability instance is Mitigated."
                : "Evidence rejected. The task went back to In Progress and the engineer was notified.");
        return "redirect:/remediation/" + id;
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        remediationService.reopen(id, reason);
        redirect.addFlashAttribute("success", "Task reopened and the engineer notified.");
        return "redirect:/remediation/" + id;
    }

    /** Access-controlled evidence download (files are not in the public static folder). */
    @GetMapping("/evidence/{evidenceId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long evidenceId) {
        RemediationService.EvidenceDownload d = remediationService.downloadEvidence(evidenceId);
        return fileResponse(d);
    }

    static ResponseEntity<Resource> fileResponse(RemediationService.EvidenceDownload d) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(d.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(d.resource());
    }
}

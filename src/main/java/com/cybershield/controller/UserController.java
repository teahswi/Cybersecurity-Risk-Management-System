package com.cybershield.controller;

import com.cybershield.dto.UserForm;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.ReferenceDataService;
import com.cybershield.service.UserService;
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

/** User management - Security Manager only (URL rule + @PreAuthorize in UserService). */
@Controller
@RequestMapping("/users")
public class UserController {

    private static final Set<String> SORTS = Set.of("fullName", "username", "email", "status", "lastLoginAt");

    private final UserService userService;
    private final ReferenceDataService referenceData;

    public UserController(UserService userService, ReferenceDataService referenceData) {
        this.userService = userService;
        this.referenceData = referenceData;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) RoleCode role,
                       @RequestParam(required = false) UserStatus status, @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "fullName") String sort, @RequestParam(defaultValue = "asc") String dir,
                       Model model) {
        model.addAttribute("page", userService.search(q, role, status, Paging.of(page, sort, dir, SORTS, "fullName")));
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("roles", RoleCode.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("u", userService.detail(id));
        return "users/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            Long id = userService.create(form);
            redirect.addFlashAttribute("success", "User " + form.getUsername() + " created.");
            return "redirect:/users/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", userService.formFor(id));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") UserForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            userService.update(id, form);
            redirect.addFlashAttribute("success", "User updated.");
            return "redirect:/users/" + id;
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            return formView(model, id);
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirect) {
        userService.setActive(id, true);
        redirect.addFlashAttribute("success", "User activated.");
        return "redirect:/users/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirect) {
        userService.setActive(id, false);
        redirect.addFlashAttribute("success", "User deactivated. They can no longer log in.");
        return "redirect:/users/" + id;
    }

    private String formView(Model model, Long id) {
        if (model.getAttribute("form") instanceof UserForm form) {
            form.setPassword(null); // never echo a password back into the page
        }
        model.addAttribute("userId", id);
        model.addAttribute("roles", RoleCode.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("departments", referenceData.departments());
        return "users/form";
    }
}

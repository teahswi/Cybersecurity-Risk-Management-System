package com.cybershield.controller;

import com.cybershield.dto.PasswordChangeForm;
import com.cybershield.dto.ProfileForm;
import com.cybershield.exception.BusinessRuleException;
import com.cybershield.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "My Profile": every user can edit permitted fields and change their password (never their role). */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profile(Model model) {
        model.addAttribute("me", userService.myProfile());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", userService.myProfileForm());
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "profile/index";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("me", userService.myProfile());
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "profile/index";
        }
        try {
            userService.updateMyProfile(form);
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("me", userService.myProfile());
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "profile/index";
        }
        redirect.addFlashAttribute("success", "Profile updated. Your new name appears after the next login.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult result,
                                 Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            clearPasswords(form);
            model.addAttribute("me", userService.myProfile());
            model.addAttribute("profileForm", userService.myProfileForm());
            return "profile/index";
        }
        try {
            userService.changeMyPassword(form);
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("me", userService.myProfile());
            model.addAttribute("profileForm", userService.myProfileForm());
            clearPasswords(form);
            return "profile/index";
        }
        redirect.addFlashAttribute("success", "Password changed.");
        return "redirect:/profile";
    }

    private static void clearPasswords(PasswordChangeForm form) {
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
    }
}

package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.service.AccountService;
import com.abhishek.quizportal.web.form.ChangePasswordForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Settings for the signed-in account (admins and students alike). */
@Controller
class AccountController {

    private final AccountService accounts;

    AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/account/password")
    String passwordPage(@ModelAttribute("form") ChangePasswordForm form) {
        return "account/password";
    }

    @PostMapping("/account/password")
    String changePassword(@SignedIn Account me, @Valid @ModelAttribute("form") ChangePasswordForm form,
                          BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return "account/password";
        }
        try {
            accounts.changePassword(me.getId(), form.getCurrentPassword(), form.getNewPassword());
        } catch (AccountService.WrongPasswordException e) {
            errors.rejectValue("currentPassword", "wrong", e.getMessage());
            return "account/password";
        }
        flash.addFlashAttribute("notice", "Password changed. Use your new password next time you sign in.");
        return "redirect:/home";
    }
}

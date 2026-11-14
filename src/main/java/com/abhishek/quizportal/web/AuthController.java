package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.service.AccountService;
import com.abhishek.quizportal.web.form.SignUpForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AuthController {

    private final AccountService accounts;

    AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/")
    String root(@SignedIn Account me) {
        return me == null ? "redirect:/signin" : "redirect:/home";
    }

    @GetMapping("/signin")
    String signIn(@SignedIn Account me) {
        return me == null ? "auth/signin" : "redirect:/home";
    }

    @GetMapping("/signup")
    String signUpPage(@ModelAttribute("form") SignUpForm form) {
        return "auth/signup";
    }

    @PostMapping("/signup")
    String signUp(@Valid @ModelAttribute("form") SignUpForm form, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return "auth/signup";
        }
        try {
            accounts.signUp(form.getDisplayName(), form.getEmail(), form.getPassword());
        } catch (AccountService.EmailTakenException e) {
            errors.rejectValue("email", "taken", e.getMessage());
            return "auth/signup";
        }
        flash.addFlashAttribute("notice", "You're registered! Sign in to start.");
        return "redirect:/signin";
    }
}

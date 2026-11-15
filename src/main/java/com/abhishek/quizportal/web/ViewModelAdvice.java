package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.service.AccountService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the signed-in account available to every template as ${me}. */
@ControllerAdvice
class ViewModelAdvice {

    private final AccountService accounts;

    ViewModelAdvice(AccountService accounts) {
        this.accounts = accounts;
    }

    @ModelAttribute("me")
    Account me() {
        return WebConfig.signedInAccount(accounts);
    }

    /** Current path, so the navigation can highlight the section you're in. */
    @ModelAttribute("path")
    String path(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI();
    }
}

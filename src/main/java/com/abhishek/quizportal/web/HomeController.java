package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.service.AccountService;
import com.abhishek.quizportal.service.AttemptService;
import com.abhishek.quizportal.service.QuizCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private final QuizCatalogService catalog;
    private final AttemptService attempts;
    private final AccountService accounts;

    HomeController(QuizCatalogService catalog, AttemptService attempts, AccountService accounts) {
        this.catalog = catalog;
        this.attempts = attempts;
        this.accounts = accounts;
    }

    @GetMapping("/home")
    String home(@SignedIn Account me, Model model) {
        if (me.isAdmin()) {
            model.addAttribute("quizTotal", catalog.quizCount());
            model.addAttribute("liveTotal", catalog.liveCount());
            model.addAttribute("studentTotal", accounts.studentCount());
            model.addAttribute("attemptTotal", attempts.finishedCount());
            model.addAttribute("average", attempts.averagePercent());
            model.addAttribute("activity", attempts.latest());
            return "home/admin";
        }
        model.addAttribute("liveTotal", catalog.liveCount());
        model.addAttribute("attemptTotal", attempts.finishedCount(me));
        model.addAttribute("average", attempts.averagePercent(me));
        model.addAttribute("activity", attempts.historyOf(me).stream().limit(5).toList());
        return "home/student";
    }

    @GetMapping("/leaderboard")
    String leaderboard(Model model) {
        model.addAttribute("rows", attempts.rankings());
        return "leaderboard";
    }
}

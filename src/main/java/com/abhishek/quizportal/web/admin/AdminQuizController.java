package com.abhishek.quizportal.web.admin;

import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.service.AttemptService;
import com.abhishek.quizportal.service.QuizCatalogService;
import com.abhishek.quizportal.web.form.QuizForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/quizzes")
class AdminQuizController {

    private final QuizCatalogService catalog;
    private final AttemptService attempts;

    AdminQuizController(QuizCatalogService catalog, AttemptService attempts) {
        this.catalog = catalog;
        this.attempts = attempts;
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("quizzes", catalog.allNewestFirst());
        return "admin/quizzes";
    }

    @GetMapping("/create")
    String createPage(Model model) {
        model.addAttribute("form", new QuizForm());
        return "admin/quiz-form";
    }

    @PostMapping("/create")
    String create(@Valid @ModelAttribute("form") QuizForm form, BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return "admin/quiz-form";
        }
        Quiz quiz = catalog.create(form);
        flash.addFlashAttribute("notice", "Quiz saved as a draft. Add questions, then publish it.");
        return "redirect:/admin/quizzes/" + quiz.getId();
    }

    /** The quiz workspace: details, questions and actions in one place. */
    @GetMapping("/{id}")
    String workspace(@PathVariable Long id, Model model) {
        model.addAttribute("quiz", catalog.quiz(id));
        return "admin/quiz-workspace";
    }

    @GetMapping("/{id}/settings")
    String settingsPage(@PathVariable Long id, Model model) {
        Quiz quiz = catalog.quiz(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("form", QuizForm.of(quiz));
        return "admin/quiz-form";
    }

    @PostMapping("/{id}/settings")
    String saveSettings(@PathVariable Long id, @Valid @ModelAttribute("form") QuizForm form,
                        BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("quiz", catalog.quiz(id));
            return "admin/quiz-form";
        }
        catalog.update(id, form);
        flash.addFlashAttribute("notice", "Settings updated.");
        return "redirect:/admin/quizzes/" + id;
    }

    @PostMapping("/{id}/toggle-live")
    String toggleLive(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Quiz quiz = catalog.toggleLive(id);
            flash.addFlashAttribute("notice", quiz.isLive() ? "Quiz is now live for students." : "Quiz moved back to drafts.");
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("problem", e.getMessage());
        }
        return "redirect:/admin/quizzes/" + id;
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes flash) {
        catalog.delete(id);
        flash.addFlashAttribute("notice", "Quiz deleted.");
        return "redirect:/admin/quizzes";
    }

    @GetMapping("/{id}/scores")
    String scores(@PathVariable Long id, Model model) {
        Quiz quiz = catalog.quiz(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts.resultsFor(quiz));
        return "admin/quiz-scores";
    }
}

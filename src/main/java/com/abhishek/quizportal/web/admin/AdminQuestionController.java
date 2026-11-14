package com.abhishek.quizportal.web.admin;

import com.abhishek.quizportal.domain.Question;
import com.abhishek.quizportal.service.QuizCatalogService;
import com.abhishek.quizportal.web.form.QuestionForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
class AdminQuestionController {

    private final QuizCatalogService catalog;

    AdminQuestionController(QuizCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/quizzes/{quizId}/questions/create")
    String createPage(@PathVariable Long quizId, Model model) {
        model.addAttribute("quiz", catalog.quiz(quizId));
        model.addAttribute("form", new QuestionForm());
        return "admin/question-form";
    }

    @PostMapping("/quizzes/{quizId}/questions/create")
    String create(@PathVariable Long quizId, @Valid @ModelAttribute("form") QuestionForm form, BindingResult errors,
                  @RequestParam(name = "next", defaultValue = "workspace") String next,
                  Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("quiz", catalog.quiz(quizId));
            return "admin/question-form";
        }
        catalog.addQuestion(quizId, form);
        flash.addFlashAttribute("notice", "Question added.");
        return "another".equals(next)
                ? "redirect:/admin/quizzes/" + quizId + "/questions/create"
                : "redirect:/admin/quizzes/" + quizId;
    }

    @GetMapping("/questions/{id}")
    String editPage(@PathVariable Long id, Model model) {
        Question question = catalog.question(id);
        model.addAttribute("quiz", question.getQuiz());
        model.addAttribute("question", question);
        model.addAttribute("form", QuestionForm.of(question));
        return "admin/question-form";
    }

    @PostMapping("/questions/{id}")
    String edit(@PathVariable Long id, @Valid @ModelAttribute("form") QuestionForm form, BindingResult errors,
                Model model, RedirectAttributes flash) {
        Question question = catalog.question(id);
        if (errors.hasErrors()) {
            model.addAttribute("quiz", question.getQuiz());
            model.addAttribute("question", question);
            return "admin/question-form";
        }
        catalog.reviseQuestion(id, form);
        flash.addFlashAttribute("notice", "Question updated.");
        return "redirect:/admin/quizzes/" + question.getQuiz().getId();
    }

    @PostMapping("/questions/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes flash) {
        Long quizId = catalog.deleteQuestion(id);
        flash.addFlashAttribute("notice", "Question removed.");
        return "redirect:/admin/quizzes/" + quizId;
    }
}

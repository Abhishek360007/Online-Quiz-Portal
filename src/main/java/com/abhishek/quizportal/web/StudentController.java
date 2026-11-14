package com.abhishek.quizportal.web;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.service.AttemptService;
import com.abhishek.quizportal.service.QuizCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** Pages for students: catalog, sitting a quiz, and reviewing results. */
@Controller
class StudentController {

    /** Answer fields are posted as "answer-{questionId}" = chosen index. */
    private static final String ANSWER_PREFIX = "answer-";

    private final QuizCatalogService catalog;
    private final AttemptService attempts;

    StudentController(QuizCatalogService catalog, AttemptService attempts) {
        this.catalog = catalog;
        this.attempts = attempts;
    }

    @GetMapping("/catalog")
    String catalog(@RequestParam(required = false) String topic,
                   @RequestParam(required = false) String find,
                   Model model) {
        model.addAttribute("quizzes", catalog.liveQuizzes(topic, find));
        model.addAttribute("topics", catalog.liveTopics());
        model.addAttribute("topic", topic);
        model.addAttribute("find", find);
        return "student/catalog";
    }

    @GetMapping("/catalog/{quizId}")
    String intro(@PathVariable Long quizId, Model model) {
        model.addAttribute("quiz", catalog.liveQuiz(quizId));
        return "student/intro";
    }

    @PostMapping("/catalog/{quizId}/begin")
    String begin(@PathVariable Long quizId, @SignedIn Account me) {
        return "redirect:/attempt/" + attempts.begin(me, quizId).getId();
    }

    @GetMapping("/attempt/{id}")
    String sit(@PathVariable Long id, @SignedIn Account me, Model model) {
        Attempt attempt = attempts.owned(me, id);
        if (attempt.isFinished()) {
            return "redirect:/attempt/" + id + "/review";
        }
        model.addAttribute("attempt", attempt);
        model.addAttribute("secondsLeft", attempts.secondsLeft(attempt));
        return "student/sit";
    }

    @PostMapping("/attempt/{id}")
    String submit(@PathVariable Long id, @SignedIn Account me, @RequestParam MultiValueMap<String, String> fields) {
        Map<Long, Integer> choices = new HashMap<>();
        fields.forEach((name, values) -> {
            if (name.startsWith(ANSWER_PREFIX) && !values.isEmpty()) {
                try {
                    choices.put(Long.valueOf(name.substring(ANSWER_PREFIX.length())), Integer.valueOf(values.get(0)));
                } catch (NumberFormatException ignored) {
                    // tampered field: leave the question unanswered
                }
            }
        });
        attempts.finish(me, id, choices);
        return "redirect:/attempt/" + id + "/review";
    }

    @GetMapping("/attempt/{id}/review")
    String review(@PathVariable Long id, @SignedIn Account me, Model model) {
        Attempt attempt = attempts.viewable(me, id);
        if (!attempt.isFinished()) {
            return "redirect:/attempt/" + id;
        }
        model.addAttribute("attempt", attempt);
        return "student/review";
    }

    @GetMapping("/my/attempts")
    String history(@SignedIn Account me, Model model) {
        model.addAttribute("attempts", attempts.historyOf(me));
        return "student/history";
    }
}

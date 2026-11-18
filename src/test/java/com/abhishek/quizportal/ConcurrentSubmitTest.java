package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.repository.AttemptRepository;
import com.abhishek.quizportal.service.AccountService;
import com.abhishek.quizportal.service.AttemptService;
import com.abhishek.quizportal.service.QuizCatalogService;
import com.abhishek.quizportal.web.form.QuestionForm;
import com.abhishek.quizportal.web.form.QuizForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two submissions of the same attempt at the same moment (a double click, two tabs, the
 * timer's auto-submit racing the button) must grade it once. Not @Transactional like the
 * other tests: each thread needs its own real transaction, so this class commits data and
 * then discards its Spring context (and the in-memory database) when it's done.
 */
@SpringBootTest
@Import(PortalTestBase.TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentSubmitTest {

    @Autowired AccountService accounts;
    @Autowired QuizCatalogService catalog;
    @Autowired AttemptService attemptService;
    @Autowired AttemptRepository attempts;
    @Autowired TransactionTemplate tx;

    @Test
    void anAttemptSubmittedTwiceAtOnceIsGradedOnce() throws Exception {
        Account student = accounts.signUp("Racer", UUID.randomUUID() + "@test.dev", "password1");
        Quiz quiz = liveQuiz();
        Attempt attempt = attemptService.begin(student, quiz.getId());
        List<Long> questionIds = tx.execute(s -> attempts.findById(attempt.getId()).orElseThrow()
                .getQuiz().getQuestions().stream().map(q -> q.getId()).toList());
        Map<Long, Integer> right = Map.of(questionIds.get(0), 1, questionIds.get(1), 0);
        Map<Long, Integer> wrong = Map.of(questionIds.get(0), 0, questionIds.get(1), 1);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> results = new ArrayList<>();
        for (Map<Long, Integer> choices : List.of(right, wrong)) {
            results.add(pool.submit(() -> {
                start.await();
                return attemptService.finish(student, attempt.getId(), choices);
            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS); // neither submission fails
        }
        pool.shutdown();

        tx.executeWithoutResult(s -> {
            Attempt graded = attempts.findById(attempt.getId()).orElseThrow();
            assertThat(graded.getAnswers()).hasSize(2);          // one answer per question, not four
            assertThat(graded.getCorrectCount()).isIn(0, 2);     // one submission's grading, whole
        });
    }

    private Quiz liveQuiz() {
        QuizForm form = new QuizForm();
        form.setTitle("Race " + UUID.randomUUID());
        form.setTopic("General");
        form.setDescription("Concurrency test");
        form.setTimeLimitMinutes(10);
        form.setPassMark(50);
        Quiz quiz = catalog.create(form);
        catalog.addQuestion(quiz.getId(), question("2 + 2 = ?", 1, "3", "4", "5"));
        catalog.addQuestion(quiz.getId(), question("Is Java statically typed?", 0, "Yes", "No"));
        catalog.toggleLive(quiz.getId());
        return quiz;
    }

    private static QuestionForm question(String prompt, int answer, String... choices) {
        QuestionForm form = new QuestionForm();
        form.setPrompt(prompt);
        form.setChoices(Arrays.asList(choices));
        form.setAnswerSlot(answer);
        form.setExplanation("Because it is.");
        return form;
    }
}

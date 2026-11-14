package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.service.AccountService;
import com.abhishek.quizportal.service.QuizCatalogService;
import com.abhishek.quizportal.web.form.QuestionForm;
import com.abhishek.quizportal.web.form.QuizForm;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Shared Spring context, a controllable clock, and helpers for building test data. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(PortalTestBase.TestClockConfig.class)
public abstract class PortalTestBase {

    protected static final String ADMIN_EMAIL = "admin@test.dev";

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.now(), ZoneId.systemDefault());
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected MutableClock clock;
    @Autowired protected AccountService accounts;
    @Autowired protected QuizCatalogService catalog;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    protected Account newStudent() {
        return accounts.signUp("Student " + UUID.randomUUID().toString().substring(0, 6),
                UUID.randomUUID() + "@test.dev", "password1");
    }

    protected RequestPostProcessor as(Account account) {
        return user(account.getEmail()).roles(account.getRole().name());
    }

    protected RequestPostProcessor asAdmin() {
        return user(ADMIN_EMAIL).roles("ADMIN");
    }

    /** A live quiz with two questions; the right answers are index 1 then index 0. */
    protected Quiz liveQuiz(String title, int minutes) {
        QuizForm form = new QuizForm();
        form.setTitle(title);
        form.setTopic("General");
        form.setDescription("A quiz used in tests");
        form.setTimeLimitMinutes(minutes);
        form.setPassMark(50);
        Quiz quiz = catalog.create(form);
        catalog.addQuestion(quiz.getId(), question("2 + 2 = ?", 1, "3", "4", "5"));
        catalog.addQuestion(quiz.getId(), question("Is Java statically typed?", 0, "Yes", "No"));
        catalog.toggleLive(quiz.getId());
        return quiz;
    }

    protected static QuestionForm question(String prompt, int answer, String... choices) {
        QuestionForm form = new QuestionForm();
        form.setPrompt(prompt);
        form.setChoices(Arrays.asList(choices));
        form.setAnswerSlot(answer);
        form.setExplanation("Because it is.");
        return form;
    }
}

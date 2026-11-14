package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Question;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.repository.AttemptRepository;
import com.abhishek.quizportal.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end: an admin builds a quiz, a student sits it through the web layer. */
class QuizFlowTest extends PortalTestBase {

    @Autowired AttemptService attempts;
    @Autowired AttemptRepository attemptRepository;

    private Long beginThroughWeb(Account student, Quiz quiz) throws Exception {
        String location = mvc.perform(post("/catalog/" + quiz.getId() + "/begin").with(as(student)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void studentSitsQuizAndIsScored() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Flow quiz", 10);
        Question q1 = quiz.getQuestions().get(0);
        Question q2 = quiz.getQuestions().get(1);

        Long attemptId = beginThroughWeb(student, quiz);
        mvc.perform(get("/attempt/" + attemptId).with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 + 2 = ?")));

        mvc.perform(post("/attempt/" + attemptId).with(as(student)).with(csrf())
                        .param("answer-" + q1.getId(), "1")
                        .param("answer-" + q2.getId(), "1"))
                .andExpect(redirectedUrl("/attempt/" + attemptId + "/review"));

        Attempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getCorrectCount()).isEqualTo(1);
        assertThat(attempt.getPercent()).isEqualTo(50);
        assertThat(attempt.isPassed()).isTrue();

        mvc.perform(get("/attempt/" + attemptId + "/review").with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("50%")))
                .andExpect(content().string(containsString("Because it is.")));
    }

    @Test
    void tamperedAnswersAreIgnored() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Tamper quiz", 10);
        Long attemptId = beginThroughWeb(student, quiz);

        mvc.perform(post("/attempt/" + attemptId).with(as(student)).with(csrf())
                        .param("answer-" + quiz.getQuestions().get(0).getId(), "99")
                        .param("answer-abc", "1"))
                .andExpect(status().is3xxRedirection());

        Attempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getCorrectCount()).isZero();
    }

    @Test
    void beginningAgainResumesTheSameAttempt() {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Resume quiz", 10);
        Long first = attempts.begin(student, quiz.getId()).getId();
        clock.advance(Duration.ofMinutes(3));
        assertThat(attempts.begin(student, quiz.getId()).getId()).isEqualTo(first);
    }

    @Test
    void staleAttemptIsClosedAndAFreshOneBegins() {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Stale quiz", 5);
        Attempt first = attempts.begin(student, quiz.getId());
        clock.advance(Duration.ofMinutes(6));

        Attempt second = attempts.begin(student, quiz.getId());

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(first.isFinished()).isTrue();
        assertThat(first.isExpired()).isTrue();
    }

    @Test
    void lateSubmissionThroughWebScoresZero() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Late quiz", 1);
        Long attemptId = beginThroughWeb(student, quiz);
        clock.advance(Duration.ofMinutes(2));

        mvc.perform(post("/attempt/" + attemptId).with(as(student)).with(csrf())
                        .param("answer-" + quiz.getQuestions().get(0).getId(), "1"))
                .andExpect(status().is3xxRedirection());

        Attempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.isExpired()).isTrue();
        assertThat(attempt.getCorrectCount()).isZero();
    }

    @Test
    void studentsCannotTouchEachOthersAttempts() throws Exception {
        Account owner = newStudent();
        Account other = newStudent();
        Quiz quiz = liveQuiz("Private quiz", 10);
        Long attemptId = beginThroughWeb(owner, quiz);

        mvc.perform(get("/attempt/" + attemptId).with(as(other))).andExpect(status().isForbidden());
        mvc.perform(post("/attempt/" + attemptId).with(as(other)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void draftQuizzesAreHiddenFromStudents() throws Exception {
        Quiz quiz = liveQuiz("Hidden quiz", 10);
        catalog.toggleLive(quiz.getId());

        Account student = newStudent();
        mvc.perform(get("/catalog").with(as(student)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Hidden quiz"))));
        mvc.perform(get("/catalog/" + quiz.getId()).with(as(student))).andExpect(status().isNotFound());
    }

    @Test
    void adminBuildsAQuizThroughTheWeb() throws Exception {
        String location = mvc.perform(post("/admin/quizzes/create").with(asAdmin()).with(csrf())
                        .param("title", "Built online").param("topic", "Web")
                        .param("timeLimitMinutes", "5").param("passMark", "70"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        Long quizId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(post("/admin/quizzes/" + quizId + "/toggle-live").with(asAdmin()).with(csrf()))
                .andExpect(flash().attributeExists("problem"));

        mvc.perform(post("/admin/quizzes/" + quizId + "/questions/create").with(asAdmin()).with(csrf())
                        .param("prompt", "Best HTTP verb for creating?")
                        .param("choices[0]", "GET").param("choices[1]", "POST").param("choices[2]", "")
                        .param("answerSlot", "1"))
                .andExpect(redirectedUrl("/admin/quizzes/" + quizId));

        mvc.perform(post("/admin/quizzes/" + quizId + "/toggle-live").with(asAdmin()).with(csrf()))
                .andExpect(flash().attributeExists("notice"));

        Quiz quiz = catalog.quiz(quizId);
        assertThat(quiz.isLive()).isTrue();
        assertThat(quiz.getQuestions()).singleElement()
                .satisfies(q -> assertThat(q.getChoices()).containsExactly("GET", "POST"));
    }

    @Test
    void invalidQuizAndQuestionFormsShowErrors() throws Exception {
        mvc.perform(post("/admin/quizzes/create").with(asAdmin()).with(csrf())
                        .param("title", "").param("topic", "").param("timeLimitMinutes", "0").param("passMark", "101"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "title", "topic", "timeLimitMinutes", "passMark"));

        Quiz quiz = liveQuiz("Errors quiz", 5);
        mvc.perform(post("/admin/quizzes/" + quiz.getId() + "/questions/create").with(asAdmin()).with(csrf())
                        .param("prompt", "").param("choices[0]", "only one"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "prompt", "enoughChoices", "answerMarked"));
    }

    @Test
    void deletingAQuestionKeepsPastAttemptsReadable() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Delete question quiz", 10);
        Attempt attempt = attempts.begin(student, quiz.getId());
        attempts.finish(student, attempt.getId(), java.util.Map.of());

        mvc.perform(post("/admin/questions/" + quiz.getQuestions().get(0).getId() + "/delete").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/quizzes/" + quiz.getId()));

        mvc.perform(get("/attempt/" + attempt.getId() + "/review").with(as(student))).andExpect(status().isOk());
    }

    @Test
    void deletingAQuizRemovesItsAttempts() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Delete quiz", 10);
        Attempt attempt = attempts.begin(student, quiz.getId());

        mvc.perform(post("/admin/quizzes/" + quiz.getId() + "/delete").with(asAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/quizzes"));

        assertThat(attemptRepository.findById(attempt.getId())).isEmpty();
    }
}

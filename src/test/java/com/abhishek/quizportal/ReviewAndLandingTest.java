package com.abhishek.quizportal;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Question;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The correction panel on the result page, and the public landing page. */
class ReviewAndLandingTest extends PortalTestBase {

    @Autowired AttemptService attempts;

    /** liveQuiz(): Q1 "2 + 2 = ?" right answer index 1 ("4"); Q2 "Is Java statically typed?" right answer index 0 ("Yes"). */
    private Attempt finish(Account student, Quiz quiz, Map<Long, Integer> choices) {
        Attempt attempt = attempts.begin(student, quiz.getId());
        return attempts.finish(student, attempt.getId(), choices);
    }

    @Test
    void wrongAnswerShowsCorrectionWithBothAnswersAndExplanation() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Correction quiz", 10);
        Question q1 = quiz.getQuestions().get(0);
        Question q2 = quiz.getQuestions().get(1);
        Attempt done = finish(student, quiz, Map.of(q1.getId(), 0, q2.getId(), 0)); // Q1 wrong ("3"), Q2 right

        assertThat(done.getWrongCount()).isEqualTo(1);
        assertThat(done.getSkippedCount()).isZero();
        assertThat(done.getMistakes()).extracting(a -> a.getQuestion().getId()).containsExactly(q1.getId());

        mvc.perform(get("/attempt/" + done.getId() + "/review").with(as(student)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Correction: why")))
                .andExpect(content().string(containsString("You answered")))
                .andExpect(content().string(containsString("A. 3")))      // their wrong pick
                .andExpect(content().string(containsString("B. 4")))      // the correct answer
                .andExpect(content().string(containsString("Because it is.")))
                .andExpect(content().string(containsString("What to review")))
                .andExpect(content().string(containsString("Mistakes (1)")))
                .andExpect(content().string(containsString("Retake quiz")));
    }

    @Test
    void skippedQuestionIsTreatedAsAMistake() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Skip quiz", 10);
        Question q1 = quiz.getQuestions().get(0);
        Attempt done = finish(student, quiz, Map.of(q1.getId(), 1)); // Q1 right, Q2 skipped

        assertThat(done.getSkippedCount()).isEqualTo(1);
        assertThat(done.getMistakes()).hasSize(1);

        mvc.perform(get("/attempt/" + done.getId() + "/review").with(as(student)))
                .andExpect(content().string(containsString("You skipped this")))
                .andExpect(content().string(containsString("A. Yes")));
    }

    @Test
    void perfectScoreHasNoCorrections() throws Exception {
        Account student = newStudent();
        Quiz quiz = liveQuiz("Perfect quiz", 10);
        Question q1 = quiz.getQuestions().get(0);
        Question q2 = quiz.getQuestions().get(1);
        Attempt done = finish(student, quiz, Map.of(q1.getId(), 1, q2.getId(), 0));

        mvc.perform(get("/attempt/" + done.getId() + "/review").with(as(student)))
                .andExpect(content().string(containsString("Perfect score")))
                .andExpect(content().string(not(containsString("Correction: why"))))
                .andExpect(content().string(containsString("Why this is right")));
    }

    @Test
    void landingPageIsPublicAndShowsLiveTopics() throws Exception {
        liveQuiz("Landing quiz", 10);
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"))
                .andExpect(content().string(containsString("Learn from every mistake")))
                .andExpect(content().string(containsString("General")));
    }

    @Test
    void signedInUsersSkipTheLandingPage() throws Exception {
        mvc.perform(get("/").with(as(newStudent()))).andExpect(redirectedUrl("/home"));
    }

    @Test
    void studentHomeSuggestsQuizzesNotYetFinished() throws Exception {
        Account student = newStudent();
        Quiz done = liveQuiz("Already done quiz", 10);
        liveQuiz("Fresh quiz", 10);
        finish(student, done, Map.of());

        mvc.perform(get("/home").with(as(student)))
                .andExpect(content().string(containsString("Try something new")))
                .andExpect(content().string(containsString("Fresh quiz")));
    }
}

package com.abhishek.quizportal.domain;

import com.abhishek.quizportal.web.form.QuestionForm;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests for the rules that live on the entities themselves. */
class DomainRulesTest {

    private final Account student = new Account("Sam", "sam@test.dev", "hash", Role.STUDENT);

    private Quiz quizWithOneQuestion(int minutes) {
        Quiz quiz = new Quiz("Rules", "Testing", null, minutes, 50);
        quiz.appendQuestion("Pick B", List.of("A", "B", "C"), 1, null);
        return quiz;
    }

    @Test
    void emptyQuizCannotGoLive() {
        Quiz quiz = new Quiz("Empty", "Testing", null, 5, 50);
        assertThatThrownBy(quiz::goLive).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removingLastQuestionTakesQuizOffline() {
        Quiz quiz = quizWithOneQuestion(5);
        quiz.goLive();
        quiz.removeQuestion(quiz.getQuestions().get(0));
        assertThat(quiz.isLive()).isFalse();
    }

    @Test
    void questionsAreNumberedInOrder() {
        Quiz quiz = quizWithOneQuestion(5);
        quiz.appendQuestion("Second", List.of("x", "y"), 0, null);
        assertThat(quiz.getQuestions()).extracting(Question::getSequence).containsExactly(1, 2);
    }

    @Test
    void questionRejectsBadChoiceSetups() {
        Quiz quiz = new Quiz("Q", "T", null, 5, 50);
        assertThatThrownBy(() -> quiz.appendQuestion("One choice", List.of("only"), 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quiz.appendQuestion("Bad answer", List.of("a", "b"), 2, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submissionAfterDeadlineIsMarkedExpiredAndScoresZero() {
        Quiz quiz = quizWithOneQuestion(10);
        LocalDateTime began = LocalDateTime.of(2026, 1, 1, 9, 0);
        Attempt attempt = new Attempt(student, quiz, began);

        attempt.finish(Map.of(), began.plusMinutes(11));

        assertThat(attempt.isExpired()).isTrue();
        assertThat(attempt.getCorrectCount()).isZero();
        assertThat(attempt.getAnswers()).allMatch(AttemptAnswer::isSkipped);
    }

    @Test
    void submissionWithinGracePeriodIsAccepted() {
        Quiz quiz = quizWithOneQuestion(10);
        LocalDateTime began = LocalDateTime.of(2026, 1, 1, 9, 0);
        Attempt attempt = new Attempt(student, quiz, began);

        attempt.finish(Map.of(), began.plusMinutes(10).plusSeconds(10));

        assertThat(attempt.isExpired()).isFalse();
    }

    @Test
    void finishingTwiceKeepsTheFirstResult() {
        Quiz quiz = quizWithOneQuestion(10);
        LocalDateTime began = LocalDateTime.of(2026, 1, 1, 9, 0);
        Attempt attempt = new Attempt(student, quiz, began);
        attempt.finish(Map.of(), began.plusMinutes(1));
        LocalDateTime first = attempt.getFinishedAt();

        attempt.finish(Map.of(), began.plusMinutes(2));

        assertThat(attempt.getFinishedAt()).isEqualTo(first);
    }

    @Test
    void questionFormDropsBlankSlotsAndRemapsTheAnswer() {
        QuestionForm form = new QuestionForm();
        form.setPrompt("  Capital of France?  ");
        form.setChoices(Arrays.asList("", "Berlin", " ", "Paris", "", ""));
        form.setAnswerSlot(3);

        QuestionForm.Cleaned cleaned = form.cleaned();

        assertThat(cleaned.prompt()).isEqualTo("Capital of France?");
        assertThat(cleaned.choices()).containsExactly("Berlin", "Paris");
        assertThat(cleaned.answerIndex()).isEqualTo(1);
    }

    @Test
    void questionFormRequiresTheAnswerToBeAFilledSlot() {
        QuestionForm form = new QuestionForm();
        form.setChoices(Arrays.asList("A", "B", "", "", "", ""));
        form.setAnswerSlot(2);
        assertThat(form.isEnoughChoices()).isTrue();
        assertThat(form.isAnswerMarked()).isFalse();
    }
}

package com.abhishek.quizportal.service;

import com.abhishek.quizportal.domain.Question;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.repository.AttemptRepository;
import com.abhishek.quizportal.repository.QuestionRepository;
import com.abhishek.quizportal.repository.QuizRepository;
import com.abhishek.quizportal.web.form.QuestionForm;
import com.abhishek.quizportal.web.form.QuizForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** Everything an admin does to quizzes and their questions, plus the student-facing catalog. */
@Service
public class QuizCatalogService {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;

    public QuizCatalogService(QuizRepository quizzes, QuestionRepository questions, AttemptRepository attempts) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.attempts = attempts;
    }

    // ---- quizzes ----

    @Transactional
    public Quiz create(QuizForm form) {
        return quizzes.save(new Quiz(form.getTitle().trim(), form.getTopic().trim(), blankToNull(form.getDescription()),
                form.getTimeLimitMinutes(), form.getPassMark()));
    }

    @Transactional
    public Quiz update(Long quizId, QuizForm form) {
        Quiz quiz = quiz(quizId);
        quiz.updateDetails(form.getTitle().trim(), form.getTopic().trim(), blankToNull(form.getDescription()),
                form.getTimeLimitMinutes(), form.getPassMark());
        return quiz;
    }

    /** Flips a quiz between draft and live. */
    @Transactional
    public Quiz toggleLive(Long quizId) {
        Quiz quiz = quiz(quizId);
        if (quiz.isLive()) {
            quiz.takeDown();
        } else {
            quiz.goLive();
        }
        return quiz;
    }

    @Transactional
    public void delete(Long quizId) {
        Quiz quiz = quiz(quizId);
        attempts.deleteAll(attempts.findByQuiz(quiz));
        quizzes.delete(quiz);
    }

    public Quiz quiz(Long quizId) {
        return quizzes.findById(quizId).orElseThrow(() -> new ResourceNotFoundException("Quiz"));
    }

    public List<Quiz> allNewestFirst() {
        return quizzes.findAllByOrderByCreatedAtDesc();
    }

    public List<Quiz> liveQuizzes(String topic, String text) {
        return quizzes.findLive(StringUtils.hasText(topic) ? topic : null,
                StringUtils.hasText(text) ? text.trim() : null);
    }

    public Quiz liveQuiz(Long quizId) {
        Quiz quiz = quiz(quizId);
        if (!quiz.isLive()) {
            throw new ResourceNotFoundException("Quiz");
        }
        return quiz;
    }

    public List<String> liveTopics() {
        return quizzes.findLiveTopics();
    }

    public long quizCount() {
        return quizzes.count();
    }

    public long liveCount() {
        return quizzes.countByLiveTrue();
    }

    // ---- questions ----

    @Transactional
    public Question addQuestion(Long quizId, QuestionForm form) {
        Quiz quiz = quiz(quizId);
        QuestionForm.Cleaned c = form.cleaned();
        Question question = quiz.appendQuestion(c.prompt(), c.choices(), c.answerIndex(), c.explanation());
        return questions.save(question);
    }

    @Transactional
    public Question reviseQuestion(Long questionId, QuestionForm form) {
        Question question = question(questionId);
        QuestionForm.Cleaned c = form.cleaned();
        question.revise(c.prompt(), c.choices(), c.answerIndex(), c.explanation());
        return question;
    }

    /** Deletes a question and returns the id of its quiz. */
    @Transactional
    public Long deleteQuestion(Long questionId) {
        Question question = question(questionId);
        Quiz quiz = question.getQuiz();
        attempts.findAnswering(question).forEach(attempt -> attempt.forget(question));
        quiz.removeQuestion(question);
        return quiz.getId();
    }

    public Question question(Long questionId) {
        return questions.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question"));
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}

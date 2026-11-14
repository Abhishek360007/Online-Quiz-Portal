package com.abhishek.quizportal.domain;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A student's sitting of one quiz, from "begin" to "finish". */
@Entity
@Table(name = "attempts")
public class Attempt {

    /** Allowance for network delay when the browser auto-submits at zero. */
    public static final Duration GRACE = Duration.ofSeconds(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(nullable = false)
    private LocalDateTime beganAt;

    private LocalDateTime finishedAt;

    private int correctCount;

    private int questionCount;

    /** True when the answers reached the server after the time limit and were discarded. */
    private boolean expired;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();

    protected Attempt() {
    }

    public Attempt(Account student, Quiz quiz, LocalDateTime beganAt) {
        this.student = student;
        this.quiz = quiz;
        this.beganAt = beganAt;
        this.questionCount = quiz.getQuestions().size();
    }

    /**
     * Grades the attempt. {@code choices} maps question id to chosen index.
     * Submissions past the deadline (plus grace) are recorded with no answers.
     */
    public void finish(Map<Long, Integer> choices, LocalDateTime now) {
        if (isFinished()) {
            return;
        }
        expired = now.isAfter(getDeadline().plus(GRACE));
        answers.clear();
        correctCount = 0;
        for (Question question : quiz.getQuestions()) {
            Integer chosen = expired || question.getId() == null ? null : choices.get(question.getId());
            if (!question.isValidChoice(chosen)) {
                chosen = null;
            }
            AttemptAnswer answer = new AttemptAnswer(this, question, chosen);
            if (answer.isCorrect()) {
                correctCount++;
            }
            answers.add(answer);
        }
        questionCount = quiz.getQuestions().size();
        finishedAt = now;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public LocalDateTime getDeadline() {
        return beganAt.plusMinutes(quiz.getTimeLimitMinutes());
    }

    public boolean isOverdue(LocalDateTime now) {
        return now.isAfter(getDeadline());
    }

    public long secondsLeft(LocalDateTime now) {
        return Math.max(0, Duration.between(now, getDeadline()).toSeconds());
    }

    public int getPercent() {
        return questionCount == 0 ? 0 : Math.round(100f * correctCount / questionCount);
    }

    public boolean isPassed() {
        return getPercent() >= quiz.getPassMark();
    }

    public String getDurationText() {
        if (finishedAt == null) {
            return "-";
        }
        Duration taken = Duration.between(beganAt, finishedAt);
        return taken.toMinutes() + "m " + taken.toSecondsPart() + "s";
    }

    /** Drops the stored answer to a question that is being deleted; the score is left as it was. */
    public void forget(Question question) {
        answers.removeIf(a -> a.getQuestion().getId().equals(question.getId()));
    }

    public Optional<AttemptAnswer> answerFor(Question question) {
        return answers.stream().filter(a -> a.getQuestion().getId().equals(question.getId())).findFirst();
    }

    public Long getId() { return id; }
    public Account getStudent() { return student; }
    public Quiz getQuiz() { return quiz; }
    public LocalDateTime getBeganAt() { return beganAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public int getCorrectCount() { return correctCount; }
    public int getQuestionCount() { return questionCount; }
    public boolean isExpired() { return expired; }
    public List<AttemptAnswer> getAnswers() { return answers; }
}

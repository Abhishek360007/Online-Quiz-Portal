package com.abhishek.quizportal.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    /** Zero-based choice, or null when skipped. */
    private Integer chosenIndex;

    private boolean correct;

    protected AttemptAnswer() {
    }

    AttemptAnswer(Attempt attempt, Question question, Integer chosenIndex) {
        this.attempt = attempt;
        this.question = question;
        this.chosenIndex = chosenIndex;
        this.correct = question.isAnsweredBy(chosenIndex);
    }

    public boolean isSkipped() {
        return chosenIndex == null;
    }

    public Long getId() { return id; }
    public Attempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public Integer getChosenIndex() { return chosenIndex; }
    public boolean isCorrect() { return correct; }
}

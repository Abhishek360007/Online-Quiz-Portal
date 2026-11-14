package com.abhishek.quizportal.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A multiple-choice question with 2 to 6 choices, exactly one of which is right. */
@Entity
@Table(name = "questions")
public class Question {

    public static final int MIN_CHOICES = 2;
    public static final int MAX_CHOICES = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    /** Display order within the quiz, starting at 1. */
    private int sequence;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_choices", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "choice_index")
    @Column(name = "label", nullable = false, length = 300)
    private List<String> choices = new ArrayList<>();

    /** Zero-based position of the right choice. */
    private int answerIndex;

    @Column(length = 1000)
    private String explanation;

    protected Question() {
    }

    Question(Quiz quiz, int sequence, String prompt, List<String> choices, int answerIndex, String explanation) {
        this.quiz = quiz;
        this.sequence = sequence;
        revise(prompt, choices, answerIndex, explanation);
    }

    public void revise(String prompt, List<String> choices, int answerIndex, String explanation) {
        if (choices.size() < MIN_CHOICES || choices.size() > MAX_CHOICES) {
            throw new IllegalArgumentException("A question needs between 2 and 6 choices.");
        }
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new IllegalArgumentException("The correct answer must be one of the choices.");
        }
        this.prompt = prompt;
        this.choices.clear();
        this.choices.addAll(choices);
        this.answerIndex = answerIndex;
        this.explanation = explanation;
    }

    public boolean isAnsweredBy(Integer choiceIndex) {
        return choiceIndex != null && choiceIndex == answerIndex;
    }

    public boolean isValidChoice(Integer choiceIndex) {
        return choiceIndex != null && choiceIndex >= 0 && choiceIndex < choices.size();
    }

    public String getAnswerLabel() {
        return choices.get(answerIndex);
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public int getSequence() { return sequence; }
    public String getPrompt() { return prompt; }
    public List<String> getChoices() { return choices; }
    public int getAnswerIndex() { return answerIndex; }
    public String getExplanation() { return explanation; }
}

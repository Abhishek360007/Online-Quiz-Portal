package com.abhishek.quizportal.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 60)
    private String topic;

    @Column(length = 600)
    private String description;

    /** Minutes a student gets once the attempt has begun. */
    private int timeLimitMinutes;

    /** Minimum percentage needed to pass. */
    private int passMark;

    /** Drafts are hidden from students. */
    private boolean live;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {
    }

    public Quiz(String title, String topic, String description, int timeLimitMinutes, int passMark) {
        this.createdAt = LocalDateTime.now();
        updateDetails(title, topic, description, timeLimitMinutes, passMark);
    }

    public void updateDetails(String title, String topic, String description, int timeLimitMinutes, int passMark) {
        this.title = title;
        this.topic = topic;
        this.description = description;
        this.timeLimitMinutes = timeLimitMinutes;
        this.passMark = passMark;
    }

    public Question appendQuestion(String prompt, List<String> choices, int answerIndex, String explanation) {
        int next = questions.stream().mapToInt(Question::getSequence).max().orElse(0) + 1;
        Question question = new Question(this, next, prompt, choices, answerIndex, explanation);
        questions.add(question);
        return question;
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        if (questions.isEmpty()) {
            live = false;
        }
    }

    public void goLive() {
        if (questions.isEmpty()) {
            throw new IllegalStateException("Add at least one question before publishing.");
        }
        live = true;
    }

    public void takeDown() {
        live = false;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public int getPassMark() { return passMark; }
    public boolean isLive() { return live; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Question> getQuestions() { return questions; }
}

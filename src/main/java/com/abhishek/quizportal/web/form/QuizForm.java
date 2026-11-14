package com.abhishek.quizportal.web.form;

import com.abhishek.quizportal.domain.Quiz;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuizForm {

    @NotBlank(message = "Give the quiz a title")
    @Size(max = 120, message = "Keep the title under 120 characters")
    private String title;

    @NotBlank(message = "Pick a topic, e.g. Java or Aptitude")
    @Size(max = 60, message = "Keep the topic under 60 characters")
    private String topic;

    @Size(max = 600, message = "Keep the description under 600 characters")
    private String description;

    @Min(value = 1, message = "At least 1 minute")
    @Max(value = 240, message = "At most 240 minutes")
    private int timeLimitMinutes = 15;

    @Min(value = 0, message = "Between 0 and 100")
    @Max(value = 100, message = "Between 0 and 100")
    private int passMark = 60;

    public static QuizForm of(Quiz quiz) {
        QuizForm form = new QuizForm();
        form.title = quiz.getTitle();
        form.topic = quiz.getTopic();
        form.description = quiz.getDescription();
        form.timeLimitMinutes = quiz.getTimeLimitMinutes();
        form.passMark = quiz.getPassMark();
        return form;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }
    public int getPassMark() { return passMark; }
    public void setPassMark(int passMark) { this.passMark = passMark; }
}

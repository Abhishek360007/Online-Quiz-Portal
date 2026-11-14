package com.abhishek.quizportal.web.form;

import com.abhishek.quizportal.domain.Question;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Backs the add/edit question page. The page always shows {@link Question#MAX_CHOICES} slots;
 * blank slots are dropped when saving.
 */
public class QuestionForm {

    @NotBlank(message = "Write the question")
    @Size(max = 1000, message = "Keep the question under 1000 characters")
    private String prompt;

    private List<String> choices = blankSlots();

    /** Index of the correct slot as shown on the page (before blanks are removed). */
    private Integer answerSlot;

    @Size(max = 1000, message = "Keep the explanation under 1000 characters")
    private String explanation;

    public static QuestionForm of(Question question) {
        QuestionForm form = new QuestionForm();
        form.prompt = question.getPrompt();
        for (int i = 0; i < question.getChoices().size(); i++) {
            form.choices.set(i, question.getChoices().get(i));
        }
        form.answerSlot = question.getAnswerIndex();
        form.explanation = question.getExplanation();
        return form;
    }

    @AssertTrue(message = "Fill in at least two choices")
    public boolean isEnoughChoices() {
        return choices.stream().filter(StringUtils::hasText).count() >= Question.MIN_CHOICES;
    }

    @AssertTrue(message = "Mark which filled-in choice is correct")
    public boolean isAnswerMarked() {
        return answerSlot != null && answerSlot >= 0 && answerSlot < choices.size()
                && StringUtils.hasText(choices.get(answerSlot));
    }

    /** Trimmed values with blank slots removed and the answer index remapped. */
    public Cleaned cleaned() {
        List<String> kept = new ArrayList<>();
        int answer = -1;
        for (int i = 0; i < choices.size(); i++) {
            String choice = choices.get(i);
            if (StringUtils.hasText(choice)) {
                if (answerSlot != null && i == answerSlot) {
                    answer = kept.size();
                }
                kept.add(choice.trim());
            }
        }
        String why = StringUtils.hasText(explanation) ? explanation.trim() : null;
        return new Cleaned(prompt.trim(), kept, answer, why);
    }

    public record Cleaned(String prompt, List<String> choices, int answerIndex, String explanation) {
    }

    private static List<String> blankSlots() {
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < Question.MAX_CHOICES; i++) {
            slots.add("");
        }
        return slots;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getChoices() { return choices; }

    public void setChoices(List<String> choices) {
        List<String> slots = blankSlots();
        for (int i = 0; i < Math.min(choices.size(), slots.size()); i++) {
            slots.set(i, choices.get(i) == null ? "" : choices.get(i));
        }
        this.choices = slots;
    }

    public Integer getAnswerSlot() { return answerSlot; }
    public void setAnswerSlot(Integer answerSlot) { this.answerSlot = answerSlot; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}

package com.abhishek.quizportal.web.view;

import org.springframework.stereotype.Component;

/**
 * Small presentation helpers for templates, available as {@code ${@ui...}}.
 */
@Component("ui")
public class Ui {

    /** Hand-picked hues for the topics we know; anything else gets a stable hue from its name. */
    private static final java.util.Map<String, Integer> KNOWN = java.util.Map.ofEntries(
            java.util.Map.entry("java", 18),
            java.util.Map.entry("c++", 212),
            java.util.Map.entry("python", 48),
            java.util.Map.entry("sql", 190),
            java.util.Map.entry("web", 330),
            java.util.Map.entry("dsa", 262),
            java.util.Map.entry("computer science", 160),
            java.util.Map.entry("security", 0),
            java.util.Map.entry("tools", 28),
            java.util.Map.entry("aptitude", 290),
            java.util.Map.entry("general knowledge", 128));

    /** A hue (0-359) used to tint a topic's chip and card stripe. */
    public int hue(String topic) {
        if (topic == null) {
            return 210;
        }
        String key = topic.trim().toLowerCase(java.util.Locale.ROOT);
        Integer known = KNOWN.get(key);
        return known != null ? known : Math.floorMod(key.hashCode(), 360);
    }

    /** 0 -> "A", 1 -> "B", ... as printed on an answer sheet. */
    public String letter(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /** First letter of each of the first two words, for avatars. */
    public String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase(java.util.Locale.ROOT);
    }
}

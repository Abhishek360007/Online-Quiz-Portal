package com.abhishek.quizportal.web.view;

/** One line of the leaderboard. */
public record RankRow(String name, Long quizzesTaken, Long correctAnswers, Double averagePercent) {

    public long roundedAverage() {
        return averagePercent == null ? 0 : Math.round(averagePercent);
    }
}

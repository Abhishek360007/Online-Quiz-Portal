package com.abhishek.quizportal.service;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.repository.AttemptRepository;
import com.abhishek.quizportal.web.view.RankRow;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AttemptService {

    private final AttemptRepository attempts;
    private final QuizCatalogService catalog;
    private final Clock clock;

    public AttemptService(AttemptRepository attempts, QuizCatalogService catalog, Clock clock) {
        this.attempts = attempts;
        this.catalog = catalog;
        this.clock = clock;
    }

    /**
     * Returns the student's in-progress attempt for this quiz if time remains;
     * otherwise closes any stale one and begins a fresh attempt.
     */
    @Transactional
    public Attempt begin(Account student, Long quizId) {
        Quiz quiz = catalog.liveQuiz(quizId);
        LocalDateTime now = now();
        var open = attempts.findFirstByStudentAndQuizAndFinishedAtIsNullOrderByBeganAtDesc(student, quiz);
        if (open.isPresent()) {
            if (!open.get().isOverdue(now)) {
                return open.get();
            }
            open.get().finish(Map.of(), now);
        }
        return attempts.save(new Attempt(student, quiz, now));
    }

    @Transactional
    public Attempt finish(Account student, Long attemptId, Map<Long, Integer> choices) {
        Attempt attempt = owned(student, attemptId);
        attempt.finish(choices, now());
        return attempt;
    }

    /** The attempt, if it belongs to this student. */
    public Attempt owned(Account student, Long attemptId) {
        Attempt attempt = find(attemptId);
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("This attempt belongs to someone else.");
        }
        return attempt;
    }

    /** The attempt, if the viewer owns it or is an admin. */
    public Attempt viewable(Account viewer, Long attemptId) {
        return viewer.isAdmin() ? find(attemptId) : owned(viewer, attemptId);
    }

    public long secondsLeft(Attempt attempt) {
        return attempt.secondsLeft(now());
    }

    public List<Attempt> historyOf(Account student) {
        return attempts.findByStudentAndFinishedAtIsNotNullOrderByFinishedAtDesc(student);
    }

    public List<Attempt> resultsFor(Quiz quiz) {
        return attempts.findByQuizAndFinishedAtIsNotNullOrderByCorrectCountDescFinishedAtAsc(quiz);
    }

    public List<Attempt> latest() {
        return attempts.findTop6ByFinishedAtIsNotNullOrderByFinishedAtDesc();
    }

    public long finishedCount() {
        return attempts.countByFinishedAtIsNotNull();
    }

    public long finishedCount(Account student) {
        return attempts.countByStudentAndFinishedAtIsNotNull(student);
    }

    public long averagePercent() {
        return round(attempts.overallAveragePercent());
    }

    public long averagePercent(Account student) {
        return round(attempts.averagePercentFor(student));
    }

    public List<RankRow> rankings() {
        return attempts.rankings();
    }

    private Attempt find(Long attemptId) {
        return attempts.findById(attemptId).orElseThrow(() -> new ResourceNotFoundException("Attempt"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static long round(Double value) {
        return value == null ? 0 : Math.round(value);
    }
}

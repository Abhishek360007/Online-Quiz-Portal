package com.abhishek.quizportal.repository;

import com.abhishek.quizportal.domain.Account;
import com.abhishek.quizportal.domain.Attempt;
import com.abhishek.quizportal.domain.Question;
import com.abhishek.quizportal.domain.Quiz;
import com.abhishek.quizportal.web.view.RankRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    Optional<Attempt> findFirstByStudentAndQuizAndFinishedAtIsNullOrderByBeganAtDesc(Account student, Quiz quiz);

    List<Attempt> findByStudentAndFinishedAtIsNotNullOrderByFinishedAtDesc(Account student);

    List<Attempt> findByQuizAndFinishedAtIsNotNullOrderByCorrectCountDescFinishedAtAsc(Quiz quiz);

    List<Attempt> findTop6ByFinishedAtIsNotNullOrderByFinishedAtDesc();

    List<Attempt> findByQuiz(Quiz quiz);

    long countByFinishedAtIsNotNull();

    long countByStudentAndFinishedAtIsNotNull(Account student);

    @Query("select avg(100.0 * a.correctCount / a.questionCount) from Attempt a "
            + "where a.finishedAt is not null and a.questionCount > 0")
    Double overallAveragePercent();

    @Query("select avg(100.0 * a.correctCount / a.questionCount) from Attempt a "
            + "where a.student = :student and a.finishedAt is not null and a.questionCount > 0")
    Double averagePercentFor(@Param("student") Account student);

    @Query("""
            select new com.abhishek.quizportal.web.view.RankRow(
                a.student.displayName, count(a), sum(a.correctCount),
                avg(100.0 * a.correctCount / a.questionCount))
            from Attempt a
            where a.finishedAt is not null and a.questionCount > 0
            group by a.student.id, a.student.displayName
            order by sum(a.correctCount) desc, avg(100.0 * a.correctCount / a.questionCount) desc
            """)
    List<RankRow> rankings();

    @Query("select distinct ans.attempt from AttemptAnswer ans where ans.question = :question")
    List<Attempt> findAnswering(@Param("question") Question question);
}

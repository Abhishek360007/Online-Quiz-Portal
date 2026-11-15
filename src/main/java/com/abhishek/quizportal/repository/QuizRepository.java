package com.abhishek.quizportal.repository;

import com.abhishek.quizportal.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByOrderByCreatedAtDesc();

    /**
     * Live quizzes, optionally narrowed by topic and free text. Pass "" (never null) to skip a filter:
     * PostgreSQL cannot infer the type of a null parameter and rejects lower(null).
     */
    @Query("""
            select q from Quiz q
            where q.live = true
              and (:topic = '' or q.topic = :topic)
              and (:text = ''
                   or lower(q.title) like concat('%', lower(:text), '%')
                   or lower(coalesce(q.description, '')) like concat('%', lower(:text), '%'))
            order by q.createdAt desc
            """)
    List<Quiz> findLive(@Param("topic") String topic, @Param("text") String text);

    @Query("select distinct q.topic from Quiz q where q.live = true order by q.topic")
    List<String> findLiveTopics();

    long countByLiveTrue();

    @Query("select new com.abhishek.quizportal.web.view.TopicCount(q.topic, count(q)) "
            + "from Quiz q where q.live = true group by q.topic order by count(q) desc, q.topic")
    List<com.abhishek.quizportal.web.view.TopicCount> liveTopicCounts();

    @Query("select count(qs) from Question qs where qs.quiz.live = true")
    long countLiveQuestions();

    /** Live quizzes this student has not finished yet, newest first. */
    @Query("""
            select q from Quiz q
            where q.live = true
              and q not in (select a.quiz from Attempt a where a.student = :student and a.finishedAt is not null)
            order by q.createdAt desc
            """)
    List<Quiz> findLiveNotFinishedBy(@Param("student") com.abhishek.quizportal.domain.Account student);
}

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
}

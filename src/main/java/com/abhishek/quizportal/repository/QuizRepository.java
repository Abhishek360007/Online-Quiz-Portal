package com.abhishek.quizportal.repository;

import com.abhishek.quizportal.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByOrderByCreatedAtDesc();

    @Query("""
            select q from Quiz q
            where q.live = true
              and (:topic is null or q.topic = :topic)
              and (:text is null
                   or lower(q.title) like lower(concat('%', :text, '%'))
                   or lower(q.description) like lower(concat('%', :text, '%')))
            order by q.createdAt desc
            """)
    List<Quiz> findLive(@Param("topic") String topic, @Param("text") String text);

    @Query("select distinct q.topic from Quiz q where q.live = true order by q.topic")
    List<String> findLiveTopics();

    long countByLiveTrue();
}

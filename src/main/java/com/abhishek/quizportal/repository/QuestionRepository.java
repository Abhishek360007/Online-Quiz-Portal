package com.abhishek.quizportal.repository;

import com.abhishek.quizportal.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}

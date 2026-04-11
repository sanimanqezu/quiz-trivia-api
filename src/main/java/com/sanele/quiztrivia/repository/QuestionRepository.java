package com.sanele.quiztrivia.repository;

import com.sanele.quiztrivia.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizId(Long quizId);

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.answerOptions WHERE q.id = :id")
    Optional<Question> findByIdWithOptions(@Param("id") Long id);

    boolean existsByIdAndQuizId(Long questionId, Long quizId);
}

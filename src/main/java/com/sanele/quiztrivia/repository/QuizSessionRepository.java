package com.sanele.quiztrivia.repository;

import com.sanele.quiztrivia.entity.QuizSession;
import com.sanele.quiztrivia.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    List<QuizSession> findByQuizIdAndStatusOrderByScoreDesc(Long quizId, SessionStatus status);

    @Query("""
            SELECT qs FROM QuizSession qs
            WHERE qs.quiz.id = :quizId AND qs.status = 'COMPLETED'
            ORDER BY qs.score DESC
            """)
    List<QuizSession> findTop10ByQuizIdOrderByScoreDesc(@Param("quizId") Long quizId, Pageable pageable);

    boolean existsByIdAndStatus(Long id, SessionStatus status);
}

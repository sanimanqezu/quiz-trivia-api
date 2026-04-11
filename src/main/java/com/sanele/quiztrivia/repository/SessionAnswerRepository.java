package com.sanele.quiztrivia.repository;

import com.sanele.quiztrivia.entity.SessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionAnswerRepository extends JpaRepository<SessionAnswer, Long> {

    List<SessionAnswer> findBySessionId(Long sessionId);

    Optional<SessionAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);
}

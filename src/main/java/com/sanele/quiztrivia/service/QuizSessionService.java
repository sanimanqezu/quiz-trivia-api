package com.sanele.quiztrivia.service;

import com.sanele.quiztrivia.dto.*;
import com.sanele.quiztrivia.entity.Question;
import com.sanele.quiztrivia.entity.Quiz;
import com.sanele.quiztrivia.entity.QuizSession;
import com.sanele.quiztrivia.entity.SessionAnswer;
import com.sanele.quiztrivia.enums.SessionStatus;
import com.sanele.quiztrivia.exception.BadRequestException;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.repository.QuestionRepository;
import com.sanele.quiztrivia.repository.QuizRepository;
import com.sanele.quiztrivia.repository.QuizSessionRepository;
import com.sanele.quiztrivia.repository.SessionAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionService {

    private final QuizRepository quizRepository;
    private final QuizSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final SessionAnswerRepository sessionAnswerRepository;

    @Transactional
    public StartSessionResponse startSession(StartSessionRequest request) {
        Quiz quiz = quizRepository.findByIdWithQuestions(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", request.quizId()));

        if (quiz.getQuestions().isEmpty()) {
            throw new BadRequestException("Cannot start a session for a quiz with no questions");
        }

        QuizSession session = QuizSession.builder()
                .quiz(quiz)
                .playerName(request.playerName())
                .totalQuestions(quiz.getQuestions().size())
                .status(SessionStatus.IN_PROGRESS)
                .score(0)
                .build();

        QuizSession saved = sessionRepository.save(session);

        return new StartSessionResponse(
                saved.getId(),
                quiz.getId(),
                quiz.getTitle(),
                saved.getPlayerName(),
                saved.getTotalQuestions(),
                saved.getStartedAt(),
                saved.getStatus()
        );
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Session " + sessionId + " is already completed");
        }

        if (sessionAnswerRepository.existsBySessionIdAndQuestionId(sessionId, request.questionId())) {
            throw new BadRequestException("Question " + request.questionId() + " has already been answered in this session");
        }

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.questionId()));

        if (!question.getQuiz().getId().equals(session.getQuiz().getId())) {
            throw new BadRequestException("Question " + request.questionId() + " does not belong to the quiz in this session");
        }

        boolean isCorrect = question.getCorrectAnswer().trim().equalsIgnoreCase(request.answer().trim());
        int pointsEarned = isCorrect ? question.getPoints() : 0;

        SessionAnswer sessionAnswer = SessionAnswer.builder()
                .session(session)
                .question(question)
                .submittedAnswer(request.answer())
                .correct(isCorrect)
                .build();

        sessionAnswerRepository.save(sessionAnswer);

        if (isCorrect) {
            session.setScore(session.getScore() + pointsEarned);
            sessionRepository.save(session);
        }

        int answeredCount = sessionAnswerRepository.findBySessionId(sessionId).size();

        return new SubmitAnswerResponse(
                question.getId(),
                request.answer(),
                isCorrect,
                question.getCorrectAnswer(),
                pointsEarned,
                session.getScore(),
                answeredCount,
                session.getTotalQuestions()
        );
    }

    @Transactional
    public CompleteSessionResponse completeSession(Long sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Session " + sessionId + " is already completed");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        List<SessionAnswer> answers = sessionAnswerRepository.findBySessionId(sessionId);
        long correctCount = answers.stream().filter(SessionAnswer::isCorrect).count();

        double percentage = session.getTotalQuestions() > 0
                ? (double) correctCount / session.getTotalQuestions() * 100.0
                : 0.0;

        return new CompleteSessionResponse(
                session.getId(),
                session.getPlayerName(),
                session.getQuiz().getTitle(),
                session.getScore(),
                session.getTotalQuestions(),
                (int) correctCount,
                Math.round(percentage * 100.0) / 100.0,
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getStatus()
        );
    }

    public SessionDetailsResponse getSessionDetails(Long sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        List<SessionAnswer> answers = sessionAnswerRepository.findBySessionId(sessionId);

        List<SessionAnswerDto> answerDtos = answers.stream()
                .map(a -> new SessionAnswerDto(
                        a.getQuestion().getId(),
                        a.getQuestion().getQuestionText(),
                        a.getSubmittedAnswer(),
                        a.getQuestion().getCorrectAnswer(),
                        a.isCorrect(),
                        a.isCorrect() ? a.getQuestion().getPoints() : 0
                ))
                .toList();

        return new SessionDetailsResponse(
                session.getId(),
                session.getQuiz().getId(),
                session.getQuiz().getTitle(),
                session.getPlayerName(),
                session.getScore(),
                session.getTotalQuestions(),
                answers.size(),
                session.getStatus(),
                session.getStartedAt(),
                session.getCompletedAt(),
                answerDtos
        );
    }

    public List<LeaderboardEntry> getLeaderboard(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz", quizId);
        }

        List<QuizSession> sessions = sessionRepository.findTop10ByQuizIdOrderByScoreDesc(quizId, PageRequest.of(0, 10));

        AtomicInteger rank = new AtomicInteger(1);
        return sessions.stream()
                .map(session -> {
                    double percentage = session.getTotalQuestions() > 0
                            ? Math.round((double) session.getScore() / session.getTotalQuestions() * 10000.0) / 100.0
                            : 0.0;
                    return new LeaderboardEntry(
                            rank.getAndIncrement(),
                            session.getPlayerName(),
                            session.getScore(),
                            session.getTotalQuestions(),
                            percentage,
                            session.getCompletedAt()
                    );
                })
                .toList();
    }
}

package com.sanele.quiztrivia.service;

import com.sanele.quiztrivia.dto.*;
import com.sanele.quiztrivia.entity.*;
import com.sanele.quiztrivia.enums.QuestionType;
import com.sanele.quiztrivia.enums.SessionStatus;
import com.sanele.quiztrivia.exception.BadRequestException;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizSessionService Tests")
class QuizSessionServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizSessionRepository sessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SessionAnswerRepository sessionAnswerRepository;

    @InjectMocks
    private QuizSessionService quizSessionService;

    private Quiz testQuiz;
    private Question testQuestion;
    private QuizSession testSession;

    @BeforeEach
    void setUp() {
        testQuiz = Quiz.builder()
                .id(1L)
                .title("General Knowledge Quiz")
                .description("A fun general knowledge quiz")
                .createdAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        testQuestion = Question.builder()
                .id(10L)
                .quiz(testQuiz)
                .questionText("What is the capital of France?")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .correctAnswer("Paris")
                .points(2)
                .answerOptions(new ArrayList<>())
                .build();

        testQuiz.getQuestions().add(testQuestion);

        testSession = QuizSession.builder()
                .id(100L)
                .quiz(testQuiz)
                .playerName("Alice")
                .startedAt(LocalDateTime.now())
                .score(0)
                .totalQuestions(1)
                .status(SessionStatus.IN_PROGRESS)
                .sessionAnswers(new ArrayList<>())
                .build();
    }

    // -------------------------------------------------------------------------
    // Start Session Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("startSession()")
    class StartSessionTests {

        @Test
        @DisplayName("should start a session successfully when quiz exists and has questions")
        void startSession_success() {
            given(quizRepository.findByIdWithQuestions(1L)).willReturn(Optional.of(testQuiz));
            given(sessionRepository.save(any(QuizSession.class))).willReturn(testSession);

            StartSessionRequest request = new StartSessionRequest(1L, "Alice");
            StartSessionResponse response = quizSessionService.startSession(request);

            assertThat(response).isNotNull();
            assertThat(response.sessionId()).isEqualTo(100L);
            assertThat(response.playerName()).isEqualTo("Alice");
            assertThat(response.quizTitle()).isEqualTo("General Knowledge Quiz");
            assertThat(response.totalQuestions()).isEqualTo(1);
            assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);

            then(sessionRepository).should().save(any(QuizSession.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when quiz does not exist")
        void startSession_quizNotFound() {
            given(quizRepository.findByIdWithQuestions(99L)).willReturn(Optional.empty());

            StartSessionRequest request = new StartSessionRequest(99L, "Bob");

            assertThatThrownBy(() -> quizSessionService.startSession(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("should throw BadRequestException when quiz has no questions")
        void startSession_quizHasNoQuestions() {
            Quiz emptyQuiz = Quiz.builder()
                    .id(2L)
                    .title("Empty Quiz")
                    .questions(new ArrayList<>())
                    .build();

            given(quizRepository.findByIdWithQuestions(2L)).willReturn(Optional.of(emptyQuiz));

            StartSessionRequest request = new StartSessionRequest(2L, "Charlie");

            assertThatThrownBy(() -> quizSessionService.startSession(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no questions");
        }
    }

    // -------------------------------------------------------------------------
    // Submit Answer Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("submitAnswer()")
    class SubmitAnswerTests {

        @Test
        @DisplayName("should record a correct answer and award points")
        void submitAnswer_correctAnswer() {
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.existsBySessionIdAndQuestionId(100L, 10L)).willReturn(false);
            given(questionRepository.findById(10L)).willReturn(Optional.of(testQuestion));

            SessionAnswer savedAnswer = SessionAnswer.builder()
                    .id(1L).session(testSession).question(testQuestion)
                    .submittedAnswer("Paris").correct(true).build();
            given(sessionAnswerRepository.save(any(SessionAnswer.class))).willReturn(savedAnswer);
            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(savedAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "Paris");
            SubmitAnswerResponse response = quizSessionService.submitAnswer(100L, request);

            assertThat(response.correct()).isTrue();
            assertThat(response.pointsEarned()).isEqualTo(2);
            assertThat(response.correctAnswer()).isEqualTo("Paris");
            assertThat(response.submittedAnswer()).isEqualTo("Paris");
            assertThat(response.questionsAnswered()).isEqualTo(1);
        }

        @Test
        @DisplayName("should record a wrong answer and award zero points")
        void submitAnswer_wrongAnswer() {
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.existsBySessionIdAndQuestionId(100L, 10L)).willReturn(false);
            given(questionRepository.findById(10L)).willReturn(Optional.of(testQuestion));

            SessionAnswer savedAnswer = SessionAnswer.builder()
                    .id(1L).session(testSession).question(testQuestion)
                    .submittedAnswer("London").correct(false).build();
            given(sessionAnswerRepository.save(any(SessionAnswer.class))).willReturn(savedAnswer);
            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(savedAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "London");
            SubmitAnswerResponse response = quizSessionService.submitAnswer(100L, request);

            assertThat(response.correct()).isFalse();
            assertThat(response.pointsEarned()).isEqualTo(0);
            assertThat(response.correctAnswer()).isEqualTo("Paris");
            assertThat(response.submittedAnswer()).isEqualTo("London");
        }

        @Test
        @DisplayName("should be case-insensitive when comparing answers")
        void submitAnswer_caseInsensitiveComparison() {
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.existsBySessionIdAndQuestionId(100L, 10L)).willReturn(false);
            given(questionRepository.findById(10L)).willReturn(Optional.of(testQuestion));

            SessionAnswer savedAnswer = SessionAnswer.builder()
                    .id(1L).session(testSession).question(testQuestion)
                    .submittedAnswer("paris").correct(true).build();
            given(sessionAnswerRepository.save(any(SessionAnswer.class))).willReturn(savedAnswer);
            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(savedAnswer));

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "paris");
            SubmitAnswerResponse response = quizSessionService.submitAnswer(100L, request);

            assertThat(response.correct()).isTrue();
            assertThat(response.pointsEarned()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw BadRequestException when session is already completed")
        void submitAnswer_sessionAlreadyCompleted() {
            testSession.setStatus(SessionStatus.COMPLETED);
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "Paris");

            assertThatThrownBy(() -> quizSessionService.submitAnswer(100L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("should throw BadRequestException when question already answered")
        void submitAnswer_questionAlreadyAnswered() {
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.existsBySessionIdAndQuestionId(100L, 10L)).willReturn(true);

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "Paris");

            assertThatThrownBy(() -> quizSessionService.submitAnswer(100L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already been answered");
        }

        @Test
        @DisplayName("should throw BadRequestException when question does not belong to quiz")
        void submitAnswer_questionNotInQuiz() {
            Quiz otherQuiz = Quiz.builder().id(99L).title("Other").questions(new ArrayList<>()).build();
            Question foreignQuestion = Question.builder()
                    .id(10L).quiz(otherQuiz)
                    .questionText("Different quiz question")
                    .correctAnswer("X").points(1).build();

            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.existsBySessionIdAndQuestionId(100L, 10L)).willReturn(false);
            given(questionRepository.findById(10L)).willReturn(Optional.of(foreignQuestion));

            SubmitAnswerRequest request = new SubmitAnswerRequest(10L, "Paris");

            assertThatThrownBy(() -> quizSessionService.submitAnswer(100L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // -------------------------------------------------------------------------
    // Complete Session Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("completeSession()")
    class CompleteSessionTests {

        @Test
        @DisplayName("should complete session and calculate score correctly")
        void completeSession_success() {
            testSession.setScore(4);
            testSession.setTotalQuestions(2);
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));

            Question q2 = Question.builder().id(11L).quiz(testQuiz)
                    .questionText("Q2").correctAnswer("B").points(2)
                    .answerOptions(new ArrayList<>()).build();

            SessionAnswer a1 = SessionAnswer.builder().id(1L).session(testSession)
                    .question(testQuestion).submittedAnswer("Paris").correct(true).build();
            SessionAnswer a2 = SessionAnswer.builder().id(2L).session(testSession)
                    .question(q2).submittedAnswer("B").correct(true).build();

            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(a1, a2));
            given(sessionRepository.save(any(QuizSession.class))).willReturn(testSession);

            CompleteSessionResponse response = quizSessionService.completeSession(100L);

            assertThat(response).isNotNull();
            assertThat(response.playerName()).isEqualTo("Alice");
            assertThat(response.finalScore()).isEqualTo(4);
            assertThat(response.correctAnswers()).isEqualTo(2);
            assertThat(response.totalQuestions()).isEqualTo(2);
            assertThat(response.percentage()).isEqualTo(100.0);
            assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("should calculate partial percentage when some answers are wrong")
        void completeSession_partialScore() {
            testSession.setScore(2);
            testSession.setTotalQuestions(2);
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));

            Question q2 = Question.builder().id(11L).quiz(testQuiz)
                    .questionText("Q2").correctAnswer("B").points(2)
                    .answerOptions(new ArrayList<>()).build();

            SessionAnswer a1 = SessionAnswer.builder().id(1L).session(testSession)
                    .question(testQuestion).submittedAnswer("Paris").correct(true).build();
            SessionAnswer a2 = SessionAnswer.builder().id(2L).session(testSession)
                    .question(q2).submittedAnswer("A").correct(false).build();

            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(a1, a2));
            given(sessionRepository.save(any(QuizSession.class))).willReturn(testSession);

            CompleteSessionResponse response = quizSessionService.completeSession(100L);

            assertThat(response.correctAnswers()).isEqualTo(1);
            assertThat(response.percentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should throw BadRequestException when session is already completed")
        void completeSession_alreadyCompleted() {
            testSession.setStatus(SessionStatus.COMPLETED);
            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));

            assertThatThrownBy(() -> quizSessionService.completeSession(100L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already completed");
        }
    }

    // -------------------------------------------------------------------------
    // Leaderboard Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getLeaderboard()")
    class LeaderboardTests {

        @Test
        @DisplayName("should return top 10 sessions sorted by score descending with correct ranks")
        void getLeaderboard_success() {
            QuizSession session1 = QuizSession.builder()
                    .id(1L).quiz(testQuiz).playerName("Alice")
                    .score(10).totalQuestions(5).status(SessionStatus.COMPLETED)
                    .startedAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();
            QuizSession session2 = QuizSession.builder()
                    .id(2L).quiz(testQuiz).playerName("Bob")
                    .score(8).totalQuestions(5).status(SessionStatus.COMPLETED)
                    .startedAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();
            QuizSession session3 = QuizSession.builder()
                    .id(3L).quiz(testQuiz).playerName("Charlie")
                    .score(5).totalQuestions(5).status(SessionStatus.COMPLETED)
                    .startedAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();

            given(quizRepository.existsById(1L)).willReturn(true);
            given(sessionRepository.findTop10ByQuizIdOrderByScoreDesc(eq(1L), any(Pageable.class)))
                    .willReturn(List.of(session1, session2, session3));

            List<LeaderboardEntry> leaderboard = quizSessionService.getLeaderboard(1L);

            assertThat(leaderboard).hasSize(3);
            assertThat(leaderboard.get(0).rank()).isEqualTo(1);
            assertThat(leaderboard.get(0).playerName()).isEqualTo("Alice");
            assertThat(leaderboard.get(0).score()).isEqualTo(10);
            assertThat(leaderboard.get(1).rank()).isEqualTo(2);
            assertThat(leaderboard.get(1).playerName()).isEqualTo("Bob");
            assertThat(leaderboard.get(2).rank()).isEqualTo(3);
            assertThat(leaderboard.get(2).score()).isEqualTo(5);
        }

        @Test
        @DisplayName("should return empty leaderboard when no completed sessions exist")
        void getLeaderboard_noSessions() {
            given(quizRepository.existsById(1L)).willReturn(true);
            given(sessionRepository.findTop10ByQuizIdOrderByScoreDesc(eq(1L), any(Pageable.class))).willReturn(List.of());

            List<LeaderboardEntry> leaderboard = quizSessionService.getLeaderboard(1L);

            assertThat(leaderboard).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when quiz does not exist")
        void getLeaderboard_quizNotFound() {
            given(quizRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> quizSessionService.getLeaderboard(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -------------------------------------------------------------------------
    // Get Session Details Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getSessionDetails()")
    class GetSessionDetailsTests {

        @Test
        @DisplayName("should return session details with all answered questions")
        void getSessionDetails_success() {
            SessionAnswer answer = SessionAnswer.builder()
                    .id(1L).session(testSession).question(testQuestion)
                    .submittedAnswer("Paris").correct(true).build();

            given(sessionRepository.findById(100L)).willReturn(Optional.of(testSession));
            given(sessionAnswerRepository.findBySessionId(100L)).willReturn(List.of(answer));

            SessionDetailsResponse details = quizSessionService.getSessionDetails(100L);

            assertThat(details).isNotNull();
            assertThat(details.sessionId()).isEqualTo(100L);
            assertThat(details.playerName()).isEqualTo("Alice");
            assertThat(details.quizTitle()).isEqualTo("General Knowledge Quiz");
            assertThat(details.answers()).hasSize(1);
            assertThat(details.answers().get(0).correct()).isTrue();
            assertThat(details.answers().get(0).submittedAnswer()).isEqualTo("Paris");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when session does not exist")
        void getSessionDetails_notFound() {
            given(sessionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> quizSessionService.getSessionDetails(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}

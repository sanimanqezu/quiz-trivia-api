package com.sanele.quiztrivia.service;

import com.sanele.quiztrivia.dto.AnswerOptionDto;
import com.sanele.quiztrivia.dto.QuestionRequest;
import com.sanele.quiztrivia.dto.QuestionResponse;
import com.sanele.quiztrivia.dto.QuizRequest;
import com.sanele.quiztrivia.dto.QuizResponse;
import com.sanele.quiztrivia.entity.Question;
import com.sanele.quiztrivia.entity.Quiz;
import com.sanele.quiztrivia.enums.QuestionType;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.repository.QuestionRepository;
import com.sanele.quiztrivia.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizService Tests")
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuizService quizService;

    private Quiz testQuiz;

    @BeforeEach
    void setUp() {
        testQuiz = Quiz.builder()
                .id(1L)
                .title("General Knowledge Quiz")
                .description("A fun general knowledge quiz")
                .createdAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("createQuiz()")
    class CreateQuizTests {

        @Test
        @DisplayName("should save and return the created quiz without questions")
        void createQuiz_success() {
            given(quizRepository.save(any(Quiz.class))).willReturn(testQuiz);

            QuizResponse response = quizService.createQuiz(
                    new QuizRequest("General Knowledge Quiz", "A fun general knowledge quiz"));

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("General Knowledge Quiz");
            assertThat(response.questionCount()).isZero();
            assertThat(response.questions()).isEmpty();

            then(quizRepository).should().save(any(Quiz.class));
        }
    }

    @Nested
    @DisplayName("getAllQuizzes()")
    class GetAllQuizzesTests {

        @Test
        @DisplayName("should return all quizzes without their nested questions")
        void getAllQuizzes_success() {
            Question question = Question.builder()
                    .id(10L).quiz(testQuiz).questionText("Q?").correctAnswer("A").points(1).build();
            testQuiz.getQuestions().add(question);
            given(quizRepository.findAll()).willReturn(List.of(testQuiz));

            List<QuizResponse> responses = quizService.getAllQuizzes();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).questionCount()).isEqualTo(1);
            assertThat(responses.get(0).questions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getQuizById()")
    class GetQuizByIdTests {

        @Test
        @DisplayName("should return the quiz with its questions when found")
        void getQuizById_success() {
            Question question = Question.builder()
                    .id(10L).quiz(testQuiz)
                    .questionText("What is the capital of France?")
                    .questionType(QuestionType.MULTIPLE_CHOICE)
                    .correctAnswer("Paris").points(2)
                    .answerOptions(new ArrayList<>()).build();
            testQuiz.getQuestions().add(question);
            given(quizRepository.findByIdWithQuestions(1L)).willReturn(Optional.of(testQuiz));

            QuizResponse response = quizService.getQuizById(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.questionCount()).isEqualTo(1);
            assertThat(response.questions()).hasSize(1);
            assertThat(response.questions().get(0).questionText()).isEqualTo("What is the capital of France?");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when quiz does not exist")
        void getQuizById_notFound() {
            given(quizRepository.findByIdWithQuestions(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> quizService.getQuizById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("addQuestion()")
    class AddQuestionTests {

        @Test
        @DisplayName("should add a question with answer options to an existing quiz")
        void addQuestion_success() {
            given(quizRepository.findById(1L)).willReturn(Optional.of(testQuiz));
            given(questionRepository.save(any(Question.class))).willAnswer(inv -> inv.getArgument(0));

            QuestionRequest request = new QuestionRequest(
                    "What is the capital of France?",
                    QuestionType.MULTIPLE_CHOICE,
                    "Paris",
                    3,
                    List.of(new AnswerOptionDto(null, "Paris", "A"),
                            new AnswerOptionDto(null, "London", "B"))
            );

            QuestionResponse response = quizService.addQuestion(1L, request);

            assertThat(response.questionText()).isEqualTo("What is the capital of France?");
            assertThat(response.points()).isEqualTo(3);
            assertThat(response.answerOptions()).hasSize(2);
            assertThat(response.answerOptions().get(0).optionLabel()).isEqualTo("A");

            ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
            then(questionRepository).should().save(captor.capture());
            assertThat(captor.getValue().getQuiz()).isEqualTo(testQuiz);
        }

        @Test
        @DisplayName("should default points to 1 when a non-positive value is supplied")
        void addQuestion_defaultsPoints() {
            given(quizRepository.findById(1L)).willReturn(Optional.of(testQuiz));
            given(questionRepository.save(any(Question.class))).willAnswer(inv -> inv.getArgument(0));

            QuestionRequest request = new QuestionRequest(
                    "True or false?", QuestionType.TRUE_FALSE, "true", 0, null);

            QuestionResponse response = quizService.addQuestion(1L, request);

            assertThat(response.points()).isEqualTo(1);
            assertThat(response.answerOptions()).isEmpty();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the quiz does not exist")
        void addQuestion_quizNotFound() {
            given(quizRepository.findById(99L)).willReturn(Optional.empty());

            QuestionRequest request = new QuestionRequest(
                    "Q?", QuestionType.TRUE_FALSE, "true", 1, null);

            assertThatThrownBy(() -> quizService.addQuestion(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            then(questionRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteQuestion()")
    class DeleteQuestionTests {

        @Test
        @DisplayName("should delete a question that belongs to the quiz")
        void deleteQuestion_success() {
            given(questionRepository.existsByIdAndQuizId(10L, 1L)).willReturn(true);

            quizService.deleteQuestion(1L, 10L);

            then(questionRepository).should().deleteById(10L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the question is not in the quiz")
        void deleteQuestion_notFound() {
            given(questionRepository.existsByIdAndQuizId(10L, 1L)).willReturn(false);

            assertThatThrownBy(() -> quizService.deleteQuestion(1L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("10");

            then(questionRepository).should(never()).deleteById(any());
        }
    }
}

package com.sanele.quiztrivia.service;

import com.sanele.quiztrivia.dto.*;
import com.sanele.quiztrivia.entity.AnswerOption;
import com.sanele.quiztrivia.entity.Question;
import com.sanele.quiztrivia.entity.Quiz;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.repository.QuestionRepository;
import com.sanele.quiztrivia.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Quiz quiz = Quiz.builder()
                .title(request.title())
                .description(request.description())
                .build();
        Quiz saved = quizRepository.save(quiz);
        return toQuizResponse(saved, false);
    }

    public List<QuizResponse> getAllQuizzes() {
        return quizRepository.findAll().stream()
                .map(quiz -> toQuizResponse(quiz, false))
                .toList();
    }

    public QuizResponse getQuizById(Long id) {
        Quiz quiz = quizRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
        return toQuizResponse(quiz, true);
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));

        Question question = Question.builder()
                .quiz(quiz)
                .questionText(request.questionText())
                .questionType(request.questionType())
                .correctAnswer(request.correctAnswer())
                .points(request.points() > 0 ? request.points() : 1)
                .build();

        if (request.answerOptions() != null && !request.answerOptions().isEmpty()) {
            List<AnswerOption> options = request.answerOptions().stream()
                    .map(dto -> AnswerOption.builder()
                            .question(question)
                            .optionText(dto.optionText())
                            .optionLabel(dto.optionLabel())
                            .build())
                    .toList();
            question.getAnswerOptions().addAll(options);
        }

        Question saved = questionRepository.save(question);
        return toQuestionResponse(saved);
    }

    @Transactional
    public void deleteQuestion(Long quizId, Long questionId) {
        if (!questionRepository.existsByIdAndQuizId(questionId, quizId)) {
            throw new ResourceNotFoundException(
                    "Question with id " + questionId + " not found in quiz with id " + quizId
            );
        }
        questionRepository.deleteById(questionId);
    }

    private QuizResponse toQuizResponse(Quiz quiz, boolean includeQuestions) {
        List<QuestionResponse> questionResponses = includeQuestions
                ? quiz.getQuestions().stream().map(this::toQuestionResponse).toList()
                : List.of();

        return new QuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getCreatedAt(),
                quiz.getQuestions().size(),
                questionResponses
        );
    }

    QuestionResponse toQuestionResponse(Question question) {
        List<AnswerOptionDto> optionDtos = question.getAnswerOptions().stream()
                .map(opt -> new AnswerOptionDto(opt.getId(), opt.getOptionText(), opt.getOptionLabel()))
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getPoints(),
                optionDtos
        );
    }
}

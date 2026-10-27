package com.sanele.quiztrivia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.quiztrivia.dto.AnswerOptionDto;
import com.sanele.quiztrivia.dto.LeaderboardEntry;
import com.sanele.quiztrivia.dto.QuestionRequest;
import com.sanele.quiztrivia.dto.QuestionResponse;
import com.sanele.quiztrivia.dto.QuizRequest;
import com.sanele.quiztrivia.dto.QuizResponse;
import com.sanele.quiztrivia.enums.QuestionType;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.service.QuizService;
import com.sanele.quiztrivia.service.QuizSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private QuizSessionService quizSessionService;

    private QuizResponse sampleQuiz() {
        return new QuizResponse(1L, "General Knowledge", "A fun quiz",
                LocalDateTime.now(), 0, List.of());
    }

    @Test
    @DisplayName("POST /api/quizzes - should return 201 with the created quiz")
    void createQuiz_shouldReturnCreated() throws Exception {
        when(quizService.createQuiz(any(QuizRequest.class))).thenReturn(sampleQuiz());

        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuizRequest("General Knowledge", "A fun quiz"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("General Knowledge"));

        verify(quizService).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("POST /api/quizzes - should return 422 when the title is blank")
    void createQuiz_withBlankTitle_shouldReturnUnprocessable() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuizRequest("", "A fun quiz"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verify(quizService, never()).createQuiz(any());
    }

    @Test
    @DisplayName("GET /api/quizzes - should return the list of quizzes")
    void getAllQuizzes_shouldReturnList() throws Exception {
        when(quizService.getAllQuizzes()).thenReturn(List.of(sampleQuiz()));

        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("General Knowledge"));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id} - should return the quiz when found")
    void getQuizById_whenFound_shouldReturnOk() throws Exception {
        when(quizService.getQuizById(1L)).thenReturn(sampleQuiz());

        mockMvc.perform(get("/api/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id} - should return 404 when the quiz is not found")
    void getQuizById_whenNotFound_shouldReturnNotFound() throws Exception {
        when(quizService.getQuizById(99L)).thenThrow(new ResourceNotFoundException("Quiz", 99L));

        mockMvc.perform(get("/api/quizzes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Quiz not found with id: 99"));
    }

    @Test
    @DisplayName("POST /api/quizzes/{id}/questions - should return 201 with the created question")
    void addQuestion_shouldReturnCreated() throws Exception {
        QuestionResponse response = new QuestionResponse(10L, "Capital of France?",
                QuestionType.MULTIPLE_CHOICE, 2,
                List.of(new AnswerOptionDto(1L, "Paris", "A")));
        when(quizService.addQuestion(eq(1L), any(QuestionRequest.class))).thenReturn(response);

        QuestionRequest request = new QuestionRequest("Capital of France?",
                QuestionType.MULTIPLE_CHOICE, "Paris", 2,
                List.of(new AnswerOptionDto(null, "Paris", "A")));

        mockMvc.perform(post("/api/quizzes/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.questionText").value("Capital of France?"));

        verify(quizService).addQuestion(eq(1L), any(QuestionRequest.class));
    }

    @Test
    @DisplayName("POST /api/quizzes/{id}/questions - should return 422 when the question text is blank")
    void addQuestion_withBlankText_shouldReturnUnprocessable() throws Exception {
        QuestionRequest request = new QuestionRequest("",
                QuestionType.MULTIPLE_CHOICE, "Paris", 2, null);

        mockMvc.perform(post("/api/quizzes/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.questionText").exists());

        verify(quizService, never()).addQuestion(any(), any());
    }

    @Test
    @DisplayName("POST /api/quizzes/{id}/questions - should return 422 when an option label is invalid")
    void addQuestion_withInvalidOptionLabel_shouldReturnUnprocessable() throws Exception {
        QuestionRequest request = new QuestionRequest("Capital of France?",
                QuestionType.MULTIPLE_CHOICE, "Paris", 2,
                List.of(new AnswerOptionDto(null, "Paris", "Z")));

        mockMvc.perform(post("/api/quizzes/1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        verify(quizService, never()).addQuestion(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/quizzes/{quizId}/questions/{questionId} - should return 204 on success")
    void deleteQuestion_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/quizzes/1/questions/10"))
                .andExpect(status().isNoContent());

        verify(quizService).deleteQuestion(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/quizzes/{quizId}/questions/{questionId} - should return 404 when missing")
    void deleteQuestion_whenNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Question with id 10 not found in quiz with id 1"))
                .when(quizService).deleteQuestion(1L, 10L);

        mockMvc.perform(delete("/api/quizzes/1/questions/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/quizzes/{id}/leaderboard - should return the leaderboard")
    void getLeaderboard_shouldReturnEntries() throws Exception {
        LeaderboardEntry entry = new LeaderboardEntry(1, "Alice", 10, 5, 100.0, LocalDateTime.now());
        when(quizSessionService.getLeaderboard(1L)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/quizzes/1/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].playerName").value("Alice"));

        verify(quizSessionService).getLeaderboard(1L);
    }
}

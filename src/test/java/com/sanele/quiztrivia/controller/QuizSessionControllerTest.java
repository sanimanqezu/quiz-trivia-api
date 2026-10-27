package com.sanele.quiztrivia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanele.quiztrivia.dto.CompleteSessionResponse;
import com.sanele.quiztrivia.dto.StartSessionRequest;
import com.sanele.quiztrivia.dto.StartSessionResponse;
import com.sanele.quiztrivia.dto.SubmitAnswerRequest;
import com.sanele.quiztrivia.dto.SubmitAnswerResponse;
import com.sanele.quiztrivia.enums.SessionStatus;
import com.sanele.quiztrivia.exception.BadRequestException;
import com.sanele.quiztrivia.exception.ResourceNotFoundException;
import com.sanele.quiztrivia.service.QuizSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizSessionController.class)
class QuizSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuizSessionService quizSessionService;

    @Test
    @DisplayName("POST /api/sessions/start - should return 201 with the started session")
    void startSession_shouldReturnCreated() throws Exception {
        StartSessionResponse response = new StartSessionResponse(
                100L, 1L, "General Knowledge", "Alice", 5,
                LocalDateTime.now(), SessionStatus.IN_PROGRESS);
        when(quizSessionService.startSession(any(StartSessionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(1L, "Alice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(100))
                .andExpect(jsonPath("$.playerName").value("Alice"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(quizSessionService).startSession(any(StartSessionRequest.class));
    }

    @Test
    @DisplayName("POST /api/sessions/start - should return 422 when the player name is blank")
    void startSession_withBlankPlayerName_shouldReturnUnprocessable() throws Exception {
        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(1L, ""))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.playerName").exists());

        verify(quizSessionService, never()).startSession(any());
    }

    @Test
    @DisplayName("POST /api/sessions/start - should return 404 when the quiz does not exist")
    void startSession_whenQuizMissing_shouldReturnNotFound() throws Exception {
        when(quizSessionService.startSession(any(StartSessionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Quiz", 99L));

        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(99L, "Alice"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /api/sessions/start - should return 400 when the quiz has no questions")
    void startSession_whenQuizEmpty_shouldReturnBadRequest() throws Exception {
        when(quizSessionService.startSession(any(StartSessionRequest.class)))
                .thenThrow(new BadRequestException("Cannot start a session for a quiz with no questions"));

        mockMvc.perform(post("/api/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(1L, "Alice"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Cannot start a session for a quiz with no questions"));
    }

    @Test
    @DisplayName("POST /api/sessions/{id}/answer - should return 200 with the answer result")
    void submitAnswer_shouldReturnOk() throws Exception {
        SubmitAnswerResponse response = new SubmitAnswerResponse(
                10L, "Paris", true, "Paris", 2, 2, 1, 5);
        when(quizSessionService.submitAnswer(eq(100L), any(SubmitAnswerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/100/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest(10L, "Paris"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsEarned").value(2))
                .andExpect(jsonPath("$.currentScore").value(2));

        verify(quizSessionService).submitAnswer(eq(100L), any(SubmitAnswerRequest.class));
    }

    @Test
    @DisplayName("POST /api/sessions/{id}/answer - should return 422 when the answer is blank")
    void submitAnswer_withBlankAnswer_shouldReturnUnprocessable() throws Exception {
        mockMvc.perform(post("/api/sessions/100/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest(10L, ""))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.answer").exists());

        verify(quizSessionService, never()).submitAnswer(any(), any());
    }

    @Test
    @DisplayName("POST /api/sessions/{id}/answer - should return 400 when the question was already answered")
    void submitAnswer_whenAlreadyAnswered_shouldReturnBadRequest() throws Exception {
        when(quizSessionService.submitAnswer(eq(100L), any(SubmitAnswerRequest.class)))
                .thenThrow(new BadRequestException("Question 10 has already been answered in this session"));

        mockMvc.perform(post("/api/sessions/100/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswerRequest(10L, "Paris"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/sessions/{id}/complete - should return 200 with the summary")
    void completeSession_shouldReturnOk() throws Exception {
        CompleteSessionResponse response = new CompleteSessionResponse(
                100L, "Alice", "General Knowledge", 8, 5, 4, 80.0,
                LocalDateTime.now(), LocalDateTime.now(), SessionStatus.COMPLETED);
        when(quizSessionService.completeSession(100L)).thenReturn(response);

        mockMvc.perform(post("/api/sessions/100/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalScore").value(8))
                .andExpect(jsonPath("$.percentage").value(80.0))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(quizSessionService).completeSession(100L);
    }

    @Test
    @DisplayName("POST /api/sessions/{id}/complete - should return 404 when the session does not exist")
    void completeSession_whenMissing_shouldReturnNotFound() throws Exception {
        when(quizSessionService.completeSession(999L))
                .thenThrow(new ResourceNotFoundException("Session", 999L));

        mockMvc.perform(post("/api/sessions/999/complete"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/sessions/{id} - should return 404 when the session does not exist")
    void getSessionDetails_whenMissing_shouldReturnNotFound() throws Exception {
        when(quizSessionService.getSessionDetails(999L))
                .thenThrow(new ResourceNotFoundException("Session", 999L));

        mockMvc.perform(get("/api/sessions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Session not found with id: 999"));
    }
}

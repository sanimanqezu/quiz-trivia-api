# Quiz Trivia API

[![CI](https://github.com/sanimanqezu/quiz-trivia-api/actions/workflows/ci.yml/badge.svg)](https://github.com/sanimanqezu/quiz-trivia-api/actions/workflows/ci.yml)

A RESTful Quiz & Trivia API built with **Spring Boot 3**, **Spring Data JPA**, **PostgreSQL**, and **Flyway**. Players can create quizzes, add questions with answer options, start timed quiz sessions, submit answers with real-time scoring feedback, and view a leaderboard of top scores.

## Tech Stack

| Layer      | Technology                                |
|------------|-------------------------------------------|
| Language   | Java 25                                   |
| Framework  | Spring Boot 3.5.16                        |
| Persistence| Spring Data JPA + Hibernate               |
| Database   | PostgreSQL 16 (runtime), H2 (tests)       |
| Migrations | Flyway                                    |
| Validation | Jakarta Bean Validation                   |
| Observability | Spring Actuator (health / info)        |
| Utilities  | Lombok                                    |
| Testing    | JUnit 5 + Mockito + AssertJ               |
| Container  | Docker + Docker Compose                   |

---

## Key design points

- **Layered architecture.** `QuizController` and `QuizSessionController` stay thin; all logic lives in `QuizService` (quiz/question authoring) and `QuizSessionService` (gameplay, scoring, leaderboard). Services default to `@Transactional(readOnly = true)` and opt into write transactions explicitly.
- **Records as DTOs with validation at the edge.** Requests are immutable records annotated with Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Min`/`@Max`, `@Pattern` for A–D option labels); `@Valid` cascades into nested answer options.
- **Stateful gameplay with guardrails.** Submitting an answer rejects completed sessions, duplicate answers for the same question, and questions that do not belong to the session's quiz. Answer comparison is trimmed and case-insensitive, and the running score is persisted as play progresses.
- **RFC 9457 Problem Details.** A single `@RestControllerAdvice` returns `application/problem+json`: `ResourceNotFoundException` → 404, `BadRequestException` → 400, and bean-validation failures → 422 with per-field errors.
- **Schema owned by Flyway.** `ddl-auto` is `validate`; the schema is created and versioned by migration scripts rather than Hibernate.
- **Environment-driven configuration.** Datasource host, port, name, and credentials come from environment variables with local defaults.

---

## Project Structure

```
quiz-trivia-api/
├── src/
│   ├── main/
│   │   ├── java/com/sanele/quiztrivia/
│   │   │   ├── QuizTriviaApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── QuizController.java
│   │   │   │   └── QuizSessionController.java
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__create_tables.sql
│   └── test/
│       ├── java/com/sanele/quiztrivia/
│       │   ├── controller/
│       │   │   ├── QuizControllerTest.java
│       │   │   └── QuizSessionControllerTest.java
│       │   └── service/
│       │       ├── QuizServiceTest.java
│       │       └── QuizSessionServiceTest.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- Docker & Docker Compose

### Running locally

This service connects to the **shared portfolio PostgreSQL** instance rather than bundling its own. Start that first from the portfolio root, then run the app.

```bash
# 1. From the portfolio root, start the shared Postgres (and the portfolio-net network)
docker compose up -d

# 2. Configure the datasource (defaults shown; override as needed)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=quiz_trivia
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# 3. Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`, with health at `http://localhost:8080/actuator/health`.

### Running the container

The provided `docker-compose.yml` builds the API image and attaches it to the external `portfolio-net` network so it can reach the shared Postgres:

```bash
docker compose up --build -d
```

### Tests

```bash
./mvnw test
```

Tests are self-contained — no real database or network is required.

- **Service unit tests** — `QuizServiceTest` and `QuizSessionServiceTest` use `@ExtendWith(MockitoExtension.class)` with mocked repositories, covering quiz/question authoring, session start, answer submission (correct, wrong, case-insensitive, duplicate, foreign-question, completed-session), scoring, leaderboard ranking, and not-found cases.
- **Controller slice tests** — `QuizControllerTest` and `QuizSessionControllerTest` use `@WebMvcTest` with `@MockitoBean` services, asserting status codes, request delegation, validation failures mapping to 422, and domain exceptions mapping through the global handler (404 / 400).

Current suite: **47 tests** (26 service + 21 controller).

---

## API Reference

### Quizzes

| Method | Endpoint                              | Description                        |
|--------|---------------------------------------|------------------------------------|
| POST   | `/api/quizzes`                        | Create a new quiz                  |
| GET    | `/api/quizzes`                        | List all quizzes                   |
| GET    | `/api/quizzes/{id}`                   | Get quiz details with questions    |
| POST   | `/api/quizzes/{id}/questions`         | Add a question to a quiz           |
| DELETE | `/api/quizzes/{quizId}/questions/{questionId}` | Remove a question from a quiz |
| GET    | `/api/quizzes/{id}/leaderboard`       | Get top 10 scores for a quiz       |

### Sessions

| Method | Endpoint                              | Description                        |
|--------|---------------------------------------|------------------------------------|
| POST   | `/api/sessions/start`                 | Start a new quiz session           |
| POST   | `/api/sessions/{sessionId}/answer`    | Submit an answer                   |
| POST   | `/api/sessions/{sessionId}/complete`  | Complete a session, get final score|
| GET    | `/api/sessions/{sessionId}`           | Get session details and answers    |

---

## Example Workflow

### 1. Create a Quiz

```bash
curl -X POST http://localhost:8080/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{"title": "General Knowledge", "description": "A fun trivia quiz"}'
```

### 2. Add a Question

```bash
curl -X POST http://localhost:8080/api/quizzes/1/questions \
  -H "Content-Type: application/json" \
  -d '{
    "questionText": "What is the capital of France?",
    "questionType": "MULTIPLE_CHOICE",
    "correctAnswer": "Paris",
    "points": 2,
    "answerOptions": [
      {"optionLabel": "A", "optionText": "London"},
      {"optionLabel": "B", "optionText": "Paris"},
      {"optionLabel": "C", "optionText": "Berlin"},
      {"optionLabel": "D", "optionText": "Madrid"}
    ]
  }'
```

### 3. Start a Session

```bash
curl -X POST http://localhost:8080/api/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"quizId": 1, "playerName": "Alice"}'
```

### 4. Submit an Answer

```bash
curl -X POST http://localhost:8080/api/sessions/1/answer \
  -H "Content-Type: application/json" \
  -d '{"questionId": 1, "answer": "Paris"}'
```

Response:
```json
{
  "questionId": 1,
  "submittedAnswer": "Paris",
  "correct": true,
  "correctAnswer": "Paris",
  "pointsEarned": 2,
  "currentScore": 2,
  "questionsAnswered": 1,
  "totalQuestions": 1
}
```

### 5. Complete the Session

```bash
curl -X POST http://localhost:8080/api/sessions/1/complete
```

### 6. View Leaderboard

```bash
curl http://localhost:8080/api/quizzes/1/leaderboard
```

---

## Data Model

```
Quiz (1) ──── (*) Question (1) ──── (*) AnswerOption
  │
  └──── (*) QuizSession (1) ──── (*) SessionAnswer
```

- A **Quiz** contains many **Questions**
- Each **Question** has up to 4 **AnswerOptions** (A/B/C/D for multiple choice)
- A **QuizSession** tracks one player's attempt at a quiz
- **SessionAnswers** record each answer submitted within a session

---

## Error Handling

The API uses RFC 9457 **Problem Details** (`application/problem+json`) for all errors:

```json
{
  "type": "https://api.quiztrivia.com/errors/resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Quiz not found with id: 99",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

| Status | Scenario                                    |
|--------|---------------------------------------------|
| 201    | Resource created successfully               |
| 204    | Resource deleted successfully               |
| 400    | Bad request (e.g. session already completed)|
| 404    | Resource not found                          |
| 422    | Validation error (field-level errors returned) |
| 500    | Unexpected server error                     |

---

## License

MIT License — free to use for personal and portfolio projects.

# Quiz Trivia API

A RESTful Quiz & Trivia API built with **Spring Boot 3**, **Spring Data JPA**, **PostgreSQL**, and **Flyway**. Players can create quizzes, add questions with answer options, start timed quiz sessions, submit answers with real-time scoring feedback, and view a leaderboard of top scores.

## Tech Stack

| Layer      | Technology                                |
|------------|-------------------------------------------|
| Language   | Java 17                                   |
| Framework  | Spring Boot 3.2.5                         |
| Persistence| Spring Data JPA + Hibernate               |
| Database   | PostgreSQL 16 (runtime), H2 (tests)       |
| Migrations | Flyway                                    |
| Validation | Jakarta Bean Validation                   |
| Utilities  | Lombok                                    |
| Testing    | JUnit 5 + Mockito + AssertJ               |
| Container  | Docker + Docker Compose                   |

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
│       ├── java/com/sanele/quiztrivia/service/
│       │   └── QuizSessionServiceTest.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for containerized setup)

### Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/sanele/quiz-trivia-api.git
cd quiz-trivia-api

# Build and start all services (API + PostgreSQL)
docker-compose up --build

# The API will be available at http://localhost:8080
```

### Run Locally

1. Start a PostgreSQL instance:
   ```bash
   docker run -d \
     --name quiz-postgres \
     -e POSTGRES_DB=quiz_trivia \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     postgres:16-alpine
   ```

2. Build and run the application:
   ```bash
   mvn clean package -DskipTests
   java -jar target/quiz-trivia-api-1.0.0.jar
   ```

### Run Tests

```bash
mvn test
```

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

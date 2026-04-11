-- V1__create_tables.sql
-- Quiz Trivia API — Initial Schema

CREATE TABLE IF NOT EXISTS quizzes (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS questions (
    id            BIGSERIAL PRIMARY KEY,
    quiz_id       BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE')),
    correct_answer VARCHAR(255) NOT NULL,
    points        INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS answer_options (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL,
    option_text  TEXT NOT NULL,
    option_label VARCHAR(1) NOT NULL CHECK (option_label IN ('A', 'B', 'C', 'D')),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_sessions (
    id              BIGSERIAL PRIMARY KEY,
    quiz_id         BIGINT NOT NULL,
    player_name     VARCHAR(100) NOT NULL,
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    score           INT NOT NULL DEFAULT 0,
    total_questions INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT fk_session_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS session_answers (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT NOT NULL,
    question_id      BIGINT NOT NULL,
    submitted_answer VARCHAR(255) NOT NULL,
    correct          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_answer_session  FOREIGN KEY (session_id)  REFERENCES quiz_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT uq_session_question UNIQUE (session_id, question_id)
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_questions_quiz_id        ON questions (quiz_id);
CREATE INDEX IF NOT EXISTS idx_answer_options_question  ON answer_options (question_id);
CREATE INDEX IF NOT EXISTS idx_sessions_quiz_id         ON quiz_sessions (quiz_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status          ON quiz_sessions (status);
CREATE INDEX IF NOT EXISTS idx_sessions_score           ON quiz_sessions (quiz_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_session_answers_session  ON session_answers (session_id);

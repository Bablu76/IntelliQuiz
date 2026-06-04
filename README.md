# IntelliQuiz

A gamified, adaptive quiz generator that turns study material (PDFs) into multiple‑choice quizzes using LLMs, with role‑based access, an adaptive‑difficulty engine, gamification, and real‑time analytics.

---

## ✨ Features

- **AI quiz generation** — upload a PDF (or pick a stored resource) and generate MCQs with an LLM (Google Gemini primary, OpenAI fallback).
- **Token‑efficient generation** — relevant‑passage retrieval, result caching, and bounded model usage keep API costs low.
- **Adaptive difficulty** — the `AdaptiveEngine` adjusts difficulty per topic based on past performance.
- **Secure scoring** — quizzes are persisted server‑side and answers are graded against the stored copy (clients can't cheat by sending `isCorrect`).
- **Auth & roles** — JWT auth with refresh tokens; `STUDENT`, `TEACHER`, `ADMIN` roles.
- **Classrooms & analytics** — leaderboards, per‑student and per‑classroom analytics, gamification.
- **Deterministic fallback** — a non‑LLM `ContentQuizService` builds quizzes straight from PDF text when no API is available.

---

## 🧱 Tech Stack

| Layer | Tech |
|-------|------|
| Backend | Java 17+ (runs on 21), Spring Boot 3.5, Spring Security, Spring Data JPA, JWT (jjwt), Apache PDFBox |
| Database | PostgreSQL (default) · H2 (embedded, `local` profile) |
| Frontend | React 19, Vite 7, React Router 7, Axios, Tailwind CSS, Recharts, Framer Motion |
| Testing | JUnit 5, Mockito, Spring Security Test (backend) · Jest, Testing Library (frontend) |

---

## 📦 Project Structure

```
IntelliQuiz-main/
├── backend/                 # Spring Boot API
│   ├── src/main/java/com/intelliquiz/backend/
│   │   ├── controller/      # Auth, Quiz, Classroom, Analytics, Admin, Teacher, Resource
│   │   ├── service/         # LLMService, ContentQuizService, AnalyticsService, adaptive/
│   │   ├── model/           # JPA entities (User, Role, GeneratedQuiz, QuizAttempt, ...)
│   │   ├── repository/      # Spring Data repositories
│   │   ├── security/        # JWT filter, SecurityConfig
│   │   └── config/          # DataInitializer (seeds roles)
│   └── src/main/resources/
│       ├── application.properties           # default (PostgreSQL)
│       └── application-local.properties      # local profile (H2, port 8081)
├── frontend/                # React + Vite app
│   └── src/                 # pages, components, utils (api.js, quizApi.js, ...)
└── documentation/           # project docs, Postman collection
```

---

## 🔧 Prerequisites

- **Java 17+** (Java 21 verified)
- **Node.js 18+** and npm (Node 25 verified)
- **PostgreSQL** *(only for the default profile — not needed for `local`)*

> No global Maven needed — the project ships the Maven wrapper (`mvnw` / `mvnw.cmd`).

---

## 🚀 Running locally

### Option A — Embedded H2 (no database install required) ✅ recommended for local dev

The backend ships a `local` Spring profile that uses an embedded H2 database and runs on **port 8081**.

**Backend** (from `backend/`):
```bash
# macOS/Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```
- API: http://localhost:8081
- H2 console: http://localhost:8081/h2-console (JDBC URL `jdbc:h2:file:./data/intelliquiz`)
- Data persists in `backend/data/`.

**Frontend** (from `frontend/`):
```bash
npm install
npm run dev      # http://localhost:5173
```
The frontend reads the API base URL from `frontend/.env` (`VITE_API_URL=http://localhost:8081`).

### Option B — PostgreSQL (default profile)

1. Create the database and user expected by `application.properties`:
   ```sql
   CREATE DATABASE intelliquizdb;
   CREATE USER intelliuser WITH PASSWORD 'intellipass';
   GRANT ALL PRIVILEGES ON DATABASE intelliquizdb TO intelliuser;
   ```
2. Run without the profile:
   ```bash
   ./mvnw spring-boot:run        # API on http://localhost:8080
   ```
3. Point the frontend at it — set `VITE_API_URL=http://localhost:8080` in `frontend/.env`.

> ℹ️ On a machine where port 8080 is taken (e.g. Jenkins), use the `local` profile (port 8081) or override with `--server.port=<free port>`.

---

## 🔑 Configuration

Key settings live in `backend/src/main/resources/application.properties`:

| Property | Purpose |
|----------|---------|
| `spring.datasource.*` | PostgreSQL connection (overridden by the `local` profile) |
| `app.jwtSecret`, `app.jwtExpirationMs`, `app.jwtRefreshExpirationMs` | JWT signing & expiry |
| `llm.provider` | `gemini` (default) |
| `gemini.api.key` | Google Gemini API key |
| `openai.api.key` | OpenAI key (fallback provider) |
| `file.upload-dir` | Directory for uploaded PDFs (`uploads/`) |

> ⚠️ **Security:** do not commit real API keys. Prefer an environment variable:
> ```properties
> gemini.api.key=${GEMINI_API_KEY:}
> ```
> The app still works without a valid key — it falls back to deterministic/backup questions.

---

## 🧪 Tests

**Backend** (from `backend/`):
```bash
./mvnw test          # 15 tests — uses in‑memory H2, no external DB needed
```

**Frontend** (from `frontend/`):
```bash
npm test             # 7 tests (Jest)
```

---

## 📡 Key API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | public | Register a user |
| POST | `/auth/login` | public | Login → returns JWT (`token`) + refresh token |
| POST | `/auth/refresh` | public | Refresh access token |
| POST | `/quiz/generate/ai` | authenticated | Generate a quiz from a PDF/resource (multipart) |
| POST | `/quiz/submit` | authenticated | Submit answers, get score + next difficulty |
| GET | `/quiz/list` | authenticated | List generated quizzes |
| GET | `/quiz/get/{id}` | authenticated | Fetch a quiz by id |
| GET | `/analytics/student/**` | authenticated | Student analytics |
| GET | `/analytics/classroom/**` | TEACHER/ADMIN | Classroom analytics |

The login response uses the field **`token`** (type `Bearer`); send it as `Authorization: Bearer <token>`.

---

## 📝 Notes

- See [CHANGES.md](CHANGES.md) for the full list of recent changes, including the local‑run setup and the LLM token‑cost optimizations.
- A Postman collection is available in `documentation/IntelliQuiz.postman_collection.json`.

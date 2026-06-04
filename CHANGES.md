# Changes

All changes made to IntelliQuiz, grouped by goal. Date: **2026-06-05**.

The work covered three things:
1. Make the app **runnable on a machine without PostgreSQL** (and without the port 8080 it expected).
2. **Reduce LLM API token usage** (the main ask).
3. **Compile, test, and verify** both backend and frontend end‑to‑end.

---

## 1. Run locally without PostgreSQL (embedded H2 profile)

The backend was hard‑wired to PostgreSQL, which isn't installed here, so it couldn't start. Added an embedded **H2** option via a Spring profile — the PostgreSQL config is left as the default for production.

| File | Change |
|------|--------|
| `backend/pom.xml` | Added the **H2** dependency (`com.h2database:h2`, runtime scope). |
| `backend/src/main/resources/application-local.properties` | **New.** `local` profile: H2 file DB (`./data/intelliquiz`), H2 dialect, H2 console enabled, and `server.port=8081`. |
| `backend/src/test/resources/application.properties` | **New.** In‑memory H2 datasource + JWT properties so `@SpringBootTest` contexts boot without a real DB. |

**Why port 8081?** Port 8080 on this machine is permanently occupied by **Jenkins**, so the backend couldn't bind to it. The `local` profile uses **8081**, and the frontend was pointed at it.

| File | Change |
|------|--------|
| `frontend/.env` | `VITE_API_URL` changed from `http://localhost:8080` → `http://localhost:8081`. |

Run with: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (see [README.md](README.md)).

---

## 2. LLM token‑cost reductions

Three independent optimizations, each cutting token usage in a different way. All in the quiz‑generation path.

### 2a. Quiz caching by content hash
Identical generation requests now reuse a previously generated quiz instead of paying for the LLM again.

| File | Change |
|------|--------|
| `model/GeneratedQuiz.java` | Added `contentHash` column (SHA‑256 of content+topic+difficulty+count). Also widened `questionsJson` to `TEXT` (was `length=10000`, which could truncate a 10‑question quiz — latent bug fixed). |
| `repository/GeneratedQuizRepository.java` | Added `findFirstByContentHashOrderByGeneratedAtDesc(String)`. |
| `controller/QuizController.java` | Before calling the LLM, compute the content hash and return the cached quiz on a hit (`"cached": true`). On a miss, generate and store the hash. Added `computeContentHash(...)` helper. |

### 2b. Cheaper, bounded model usage
Stopped the fallback chain from silently escalating to expensive models, and capped output size.

| File | Change |
|------|--------|
| `service/llm/LLMService.java` | Trimmed the Gemini fallback list to **cheap "flash" tiers only** (`flash-lite`, `2.0-flash`, `flash`) — removed `gemini-2.5-pro` / `gemini-3.0-pro-preview` so a transient rate‑limit can't escalate to premium‑priced models. Added `generationConfig.maxOutputTokens = 2048` (plus `temperature`) to the Gemini request body to bound output tokens. |

### 2c. Relevant‑passage retrieval (biggest win)
Previously the **entire PDF** was sent to the LLM in 100,000‑char chunks — one paid call per chunk. Now only the passages most relevant to the topic are sent, in a **single** call.

| File | Change |
|------|--------|
| `service/llm/LLMService.java` | Replaced the chunk‑and‑loop pipeline (`createFixedChunks` + per‑chunk calls) with retrieval: `selectRelevantContext()` splits the document into 1,200‑char passages, keyword‑scores each against the topic (`scorePassage` / `tokenize`), and sends only the top passages up to `MAX_CONTEXT_CHARS = 8000` in one call. Documents already under budget pass through unchanged; if nothing matches, leading passages are used. |

**Measured impact (verified live):** a **236,499‑char** PDF was reduced to **7,210 chars** before the LLM call — **~97% fewer input tokens**, and **1 call instead of 3**.

---

## 3. Test fixes (frontend)

Two frontend tests were stale (referenced code that had been renamed/changed) and failed independently of the above work. Fixed to match the actual source — no production code changed.

| File | Change |
|------|--------|
| `frontend/src/utils/__tests__/quizApi.test.js` | Imported the non‑existent `generateQuiz`; updated to the real exports `generateAIQuiz` / `submitQuiz` and the real endpoint `/quiz/generate/ai`. |
| `frontend/src/utils/__tests__/api.test.js` | Was mocking the global `axios` instead of the `api` instance (caused network errors), and tried to redefine the locked jsdom `window.location`. Now mocks the `api` instance and asserts the 401 handler's observable side‑effect (token cleared). |

---

## ✅ Verification

| Check | Result |
|-------|--------|
| Backend build + tests | **15/15 pass** (`./mvnw test`, in‑memory H2) |
| Frontend build | Vite build succeeds |
| Frontend tests | **7/7 pass** (`npm test`) |
| Backend boot | Starts on `:8081` with H2, seeds roles |
| Auth flow | register → login (JWT) → protected `/quiz/**` returns 200; blocked (403) without token |
| Frontend dev server | Serves on `:5173` |
| Retrieval | 236,499 chars → 7,210 chars selected (logged) |

---

## ⚠️ Known issue / follow‑ups

- **Committed Gemini API key is invalid/expired.** Live generation currently falls back to backup questions (all models fail with a non‑rate‑limit auth error). Set a valid key — ideally via an env var (`gemini.api.key=${GEMINI_API_KEY:}`) — to get real generated questions. The key should also be rotated since it was committed to the repo.
- **Production parity:** the `local` profile uses H2; production still uses PostgreSQL. Schema is created by JPA (`ddl-auto=update`), so both work, but they are not identical engines.

### Suggested next steps (not yet implemented)
- Move the Gemini/OpenAI keys to environment variables.
- Add a per‑user/day generation quota to cap spend.
- Tag questions by concept for finer‑grained adaptive targeting and spaced‑repetition review.

# Online Quiz Portal

A web app for running timed multiple-choice quizzes. Admins build and publish quizzes; students sit them against the clock, get scored instantly, review every answer, and compete on a leaderboard.

Built with **Spring Boot 4**, **Spring Security**, **Thymeleaf** and **PostgreSQL**.

**Live demo:** <https://online-quiz-portal-jxsr.onrender.com>
(free hosting: if nobody has visited for a while, the first page load takes about 2 minutes while the server wakes up)

![Landing page](docs/screenshots/landing.png)

---

## Screenshots

### Taking a quiz
A sticky bar keeps the clock, the answered count and a numbered rail in view. Click any number to jump to that question.

![Taking a quiz](docs/screenshots/taking-quiz.png)

### Result and corrections
Instant score with correct / wrong / skipped counts, a list of questions to review, and a Mistakes-only filter.

![Result page](docs/screenshots/result.png)

Every wrong or skipped answer gets a correction: what you picked, the right answer and why.

<p align="center"><img src="docs/screenshots/correction.png" alt="Correction for a wrong answer" width="680"></p>

### Browsing and home
| Browse by topic | Student home |
|---|---|
| ![Browse quizzes](docs/screenshots/browse.png) | ![Student home](docs/screenshots/student-home.png) |

### Leaderboard
![Leaderboard](docs/screenshots/leaderboard.png)

### Admin
| Dashboard | Quiz workspace |
|---|---|
| ![Admin dashboard](docs/screenshots/admin-dashboard.png) | ![Quiz workspace](docs/screenshots/admin-quiz.png) |

| Question editor | Sign in |
|---|---|
| ![Question editor](docs/screenshots/admin-question.png) | ![Sign in](docs/screenshots/signin.png) |

### On a phone
<p align="center"><img src="docs/screenshots/mobile.png" alt="Landing page on a phone" width="300"></p>

---

## Features

**For students**
- Sign up and sign in (passwords hashed with BCrypt), change password from the account menu
- Browse live quizzes, search by text, filter by colour-coded topic
- Sit a quiz with a live countdown and a progress rail. Answers are submitted automatically when time runs out
- Leave and come back: an unfinished attempt resumes, and the clock keeps running on the server
- Instant score with correct / wrong / skipped counts
- **Corrections for every mistake:** your answer next to the right one, plus an explanation
- "What to review" list, Mistakes-only filter and one-click retake
- Personal history of every attempt and suggestions for quizzes not taken yet
- Leaderboard with a podium for the top three

**For admins**
- Dashboard with totals, average score and the latest attempts
- Create quizzes with a topic, description, time limit and pass mark
- Questions with 2–6 choices, a marked correct answer and an explanation that feeds the student's correction
- Draft → live publishing (a quiz needs at least one question to go live)
- Per-quiz score table showing each student's result and time used

**Under the hood**
- Role-based access: `ADMIN` and `STUDENT` see different parts of the app
- The deadline is enforced on the server (with a 15-second grace for auto-submit). Late answers aren't counted
- CSRF protection on every form; all changes are `POST` requests
- Server-side validation with messages shown next to each field
- Every page is rendered in the tests, so a broken template fails the build

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Language | Java 17+ |
| Framework | Spring Boot 4.1 (Spring MVC) |
| Security | Spring Security, BCrypt |
| Views | Thymeleaf, hand-written CSS, a little vanilla JS |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Tests | JUnit 5, MockMvc, H2 (in PostgreSQL mode) |

---

## Project layout

```
src/main/java/com/abhishek/quizportal
├── QuizPortalApp.java        entry point
├── config/                   security, clock, first-admin bootstrap
├── domain/                   Account, Quiz, Question, Attempt, AttemptAnswer
├── repository/               Spring Data repositories
├── service/                  AccountService, QuizCatalogService, AttemptService
└── web/                      controllers, form objects, view models
    └── admin/                admin-only controllers

src/main/resources
├── templates/                auth/, home/, admin/, student/, fragments/
├── static/assets/            app.css, timer.js
└── application.properties
```

---

## Running with Docker (recommended)

The only requirement is Docker. Compose starts PostgreSQL and the app together.

```bash
cp .env.example .env        # then set your own passwords in .env
docker compose up -d --build
```

Open <http://localhost:8080> and sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` from your `.env`.

| Command | What it does |
|---------|--------------|
| `docker compose logs -f app` | Follow the application logs |
| `docker compose ps` | Show both containers and their health |
| `docker compose down` | Stop everything (data is kept in the `pgdata` volume) |
| `docker compose down -v` | Stop and **delete** all data |

The image is a multi-stage build: Maven compiles the jar, then it runs on a slim Java 21 JRE as a non-root user. A health check at `/actuator/health` reports `UP` once the database connection works.

### Free deployment: Render + Neon

The repo includes a [`render.yaml`](render.yaml) blueprint, so Render builds the Docker image itself.

**1. Database on [Neon](https://neon.tech)**
1. Create a free project. Pick the **AWS Asia Pacific (Singapore)** region so it sits next to the Render service.
2. Open **Connect**, turn **Connection pooling off** and copy the connection string. It looks like
   `postgresql://USER:PASSWORD@ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require`
3. Split it into three values:
   - `DB_URL` = `jdbc:postgresql://ep-xxxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
   - `DB_USERNAME` = `USER`
   - `DB_PASSWORD` = `PASSWORD`

**2. App on [Render](https://render.com)**
1. Sign in with GitHub and choose **New → Blueprint**.
2. Select this repository. Render reads `render.yaml` and asks for the secret values.
3. Enter the three Neon values above, plus an `ADMIN_EMAIL` and a strong `ADMIN_PASSWORD`.
4. Click **Apply**. The first build takes a few minutes. The app is live at `https://online-quiz-portal-xxxx.onrender.com` once the health check at `/actuator/health` passes.

**What to expect on the free tiers**
- Render puts the service to sleep after 15 minutes without traffic. The next visit wakes it in about 2 minutes, and pages are fast after that.
- Neon suspends the database when it's idle and resumes it in about a second. The connection pool is configured to let it sleep.
- Each push to `main` redeploys automatically.

The image starts faster on small CPUs because it includes a class-data-sharing archive generated during the build. Under Render-free-like limits (0.1 CPU, 512 MB), startup drops from about 7 minutes to about 2.

### Other hosts

Any container host works (Railway, Fly.io, a VPS). Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` and, if the platform assigns one, `PORT`. Use `/actuator/health` as the health-check path.

---

## Running without Docker

### 1. Requirements
- JDK 17 or newer
- PostgreSQL 14 or newer

### 2. Create a database

```sql
CREATE DATABASE quiz_portal;
```

Tables are created automatically on first start.

### 3. Configure (optional)

The defaults connect to `localhost:5432/quiz_portal` as `postgres` / `postgres`. Override them with environment variables:

| Variable | Default |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/quiz_portal` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `ADMIN_EMAIL` | `admin@quizportal.dev` |
| `ADMIN_PASSWORD` | `ChangeMe@2026` |
| `PORT` | `8080` |

### 4. Start

```bash
./mvnw spring-boot:run       # macOS / Linux
mvnw.cmd spring-boot:run     # Windows
```

Open <http://localhost:8080> and sign in with the admin email and password above. Create a quiz, add questions and publish it. Then sign up as a student in another browser to take it.

> Set your own `ADMIN_PASSWORD` before deploying anywhere public.

### 5. Tests

```bash
./mvnw test
```

The tests use an in-memory H2 database, so they don't need PostgreSQL.

---

## Roadmap

- Shuffle question and choice order
- Import questions from CSV
- Charts on the admin dashboard
- Password reset by email

---

## Author

**Abhishek Raj** · [github.com/Abhishek360007](https://github.com/Abhishek360007)

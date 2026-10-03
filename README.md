# BBAU Library Management System

A Java 21 and Spring Boot library management API backed by Supabase PostgreSQL, with a responsive Vite and React dashboard.

## Roles and account flow

- Students use public registration. New requests start as `PENDING` and remain inactive until a member of **Administration** approves them.
- **Administration** manages the book catalogue and loans, and reviews student registrations.
- **Super Admin** creates Administration accounts and manages system-level settings.
- Administration can add/delete catalogue titles, record a student fine and confirm a submitted fee.
- Students can view fines on their account and submit due fees for Administration to review.
- The dashboard starts with sample catalogue, member, loan, and fine records so each screen has data to explore.
- Students can browse the catalogue and request a loan; Administration can issue books to a member, process returns, and review member accounts.
- Students and Administration can use the OCR scanner to read a printed book title or `BK-<ID>` label, verify the match, then issue or return that book. The browser asks for camera permission only after **Start camera** is pressed; an image upload is also available.

The supplied Babasaheb Bhimrao Ambedkar University crest is included in `public/university-logo.svg` and appears on the sign-in screen and dashboard.

## Run locally

Start the Java API on port 8080:

```powershell
.\start-api.ps1
```

The script asks for the Supabase database password without echoing it, percent-encodes it, and starts the Java API using the supplied Session Pooler host (port 5432). It does not save the password. To avoid OneDrive build-output permission issues, it copies the Java sources to a temporary Windows folder before building. The API stores accounts, students, administrators, books, issues/returns, fines, and ID counters in Supabase PostgreSQL. The schema and demo records are initialized at startup. Do not use the transaction pooler for this Spring JDBC backend.

In a second terminal, start the dashboard on port 5173:

```powershell
npm install
npm run dev
```

Vite forwards `/api` requests to Spring Boot.
The first OCR scan downloads the English recognition model from the configured Tesseract.js CDN and may need an internet connection.

## Java API

- `POST /api/auth/login` — sign in with `email`, `password`, and `role`; returns a bearer session token.
- `POST /api/auth/logout` — end the active bearer session.
- `POST /api/students/register` — public student registration with a password; the account remains pending until Administration approves it.
- Protected actions use `Authorization: Bearer <token>`; the selected role alone does not grant access.
- `GET /api/admin/students?pendingOnly=true` — Administration review queue.
- `POST /api/admin/students/{id}/review` — Administration approval or rejection (`{"approve":true}`).
- `POST /api/super-admin/administrators` — create an Administration login as Super Admin; provide the initial password.
- `POST /api/admin/books`, `POST /api/admin/loans`, and `POST /api/admin/loans/{id}/return` — catalogue and circulation actions.
- `DELETE /api/admin/books?title=...` — remove a catalogue title when there are no active loans against it.
- `POST /api/admin/fines` — record a student fine (`studentId`, `reason`, `amount`).
- `POST /api/student/fines/{id}/submit` — student submits a due fine fee for review.
- `POST /api/admin/fines/{id}/confirm` — Administration confirms a submitted payment.
- `GET /api/books` — browse available titles.

## Demo data and limits

The sample student list includes Manpreet Singh (`STU-2024-018`), Milan Kumar (`STU-2023-104`), and Priya Sharma (`STU-2025-027`, pending Administration approval). Use the registration flow or credentials configured privately in the deployment environment; login credentials are intentionally not documented in this public repository. Student registration passwords and staff account passwords are stored as salted PBKDF2 hashes in Supabase PostgreSQL. Library records persist across API restarts; bearer sessions remain in memory and users must sign in again after a restart.

For deployment, set `SUPER_ADMIN_EMAIL` and `SUPER_ADMIN_PASSWORD` as private backend environment variables. On startup, the backend creates or refreshes that Super Admin account and removes the built-in demo Super Admin account if a different email is configured. Never put the Super Admin password in GitHub source or a frontend `VITE_` variable.

# BBAU Library Management System

A Java 21 and Spring Boot library management API with a responsive browser dashboard prototype.

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
mvn spring-boot:run
```

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

## Demo access and limits

Use these accounts with the matching account type selected on the sign-in screen:

| Account type | Email | Password |
| --- | --- | --- |
| Student | `manpreet@bbau.ac.in` | `Student@123` |
| Administration | `meera.joshi@bbau.ac.in` | `Library@123` |
| Super Admin | `superadmin@bbau.ac.in` | `Admin@123` |

The sample student list includes Manpreet Singh (`STU-2024-018`), Milan Kumar (`STU-2023-104`), and Priya Sharma (`STU-2025-027`, pending Administration approval). Student registration passwords and staff account passwords are stored as salted PBKDF2 hashes. This project is still a classroom prototype: users, sessions, books and circulation records are held in memory, so they reset when the Java API restarts. Demo passwords are public by design; change them and add persistent storage, rate limiting, and production-grade session controls before deployment.


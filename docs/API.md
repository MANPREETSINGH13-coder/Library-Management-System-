# API notes

Base URL: `/api`

- `POST /auth/login`
- `POST /students/register`
- `GET /books`
- `GET /student/profile` — returns only the signed-in student's registration details; the student ID is derived from the bearer token. No password fields are returned.
- `POST /admin/books`
- `POST /admin/loans`
- `POST /admin/loans/{id}/return`
- `POST /admin/fines`
- `POST /student/fines/{id}/submit`

Protected endpoints require `Authorization: Bearer <token>`.
- `GET /super-admin/students` — Super Admin read-only registration summaries (ID, name, enrollment, email, status). Approval endpoints remain Administration-only.

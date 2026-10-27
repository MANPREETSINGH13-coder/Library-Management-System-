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

- `PUT /student/profile` — signed-in Student can update whitelisted personal/academic/contact fields and consent. Identity comes from the token. Email, enrollment, role, approval status and password hash cannot be changed here. Blank legacy fields can be completed; nonempty mobile and Aadhaar values require 10 and 12 digits. Display name is synchronized to the login account. Password changes use `PUT /auth/profile`.

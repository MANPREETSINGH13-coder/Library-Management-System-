# API notes

Base URL: `/api`

- `POST /auth/login`
- `POST /students/register`
- `GET /books`
- `POST /admin/books`
- `POST /admin/loans`
- `POST /admin/loans/{id}/return`
- `POST /admin/fines`
- `POST /student/fines/{id}/submit`

Protected endpoints require `Authorization: Bearer <token>`.
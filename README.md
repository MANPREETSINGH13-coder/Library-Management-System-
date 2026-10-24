# BBAU Library Management System

A full-stack library portal for BBAU University with separate Student, Administration, and Super Admin workflows.

## Repository structure

- `backend/` — Java 21 Spring Boot REST API and MongoDB Atlas integration.
- `frontend/` — React + Vite responsive web portal.
- `docs/` — API and database notes.
- `render.yaml` — Render Blueprint for API and frontend services.

## Performance improvements

- Login opens the dashboard immediately while dashboard data refreshes in the background.
- OCR (`tesseract.js`) is loaded only when the scanner is used.
- Frontend and backend build independently, reducing deployment work and keeping service boundaries clear.
- Static assets are served from the frontend build instead of the API process.
- Render free-service cold starts are handled without blocking the login screen.

## Local development

Backend:

```powershell
cd backend
..\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Set `MONGODB_URI`, `MONGODB_DATABASE`, `SUPER_ADMIN_EMAIL`, and `SUPER_ADMIN_PASSWORD` as private backend environment variables. Never commit passwords or MongoDB connection strings.

## Deployment

- GitHub Pages workflow builds `frontend/` and publishes `frontend/dist`.
- Render uses `render.yaml` to deploy the Docker API and static frontend.
- The API health endpoint is `/api/books`.
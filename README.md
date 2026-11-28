# BBAU Library Management System

The primary user interface is a JavaFX desktop application. It has Student, Administration, and Super Admin dashboards and connects to the Java Spring Boot API backed by MongoDB.

## Components

- `desktop-frontend/` — JavaFX desktop client, registration form, role dashboards, profile editing, and circulation workflows.
- `backend/` — Java 21 Spring Boot REST API and MongoDB integration.
- `docs/` — API and database documentation.
- `render.yaml` — Render Blueprint for the API service.

## Run on Windows

Install JDK 21 or later. Set the API database connection and start the API in one PowerShell window:

```powershell
$env:MONGODB_URI = 'mongodb+srv://<user>:<password>@<cluster>/<database>?retryWrites=true&w=majority'
.\start-api.ps1
```

In a second PowerShell window, launch the JavaFX client:

```powershell
$env:LIBRARY_API_URL = 'http://localhost:8080'
.\desktop-frontend\start.ps1
```

For a deployed API, set `LIBRARY_API_URL` to that API's base URL. Do not commit database credentials or user passwords.

## JavaFX dashboards

- Student: book catalogue, issue and return requests, OCR scanning from camera or image, fines, profile and academic details.
- Administration: student member list, registration approvals, catalogue management, OCR camera/image scanning, issue/return desk, fines, and reports.
- Super Admin: registered students, Administration accounts, and API status.
- Every role: account profile, password change, and an account-specific profile photo stored on the current computer.

## Browser applets

Java applets cannot run in current mainstream browsers. The Java Applet API is deprecated for removal and has no replacement; this application uses JavaFX as a native desktop frontend instead. The React source is removed. GitHub Pages contains a short desktop-app guide only; it cannot run the JavaFX interface in a browser.

## Deployment

Render deploys the Spring Boot API only. The JavaFX interface runs on a user's computer and must be launched with the script above. The API health endpoint is `/api/health`.

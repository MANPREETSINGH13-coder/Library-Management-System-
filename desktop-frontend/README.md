# BBAU JavaFX Desktop Frontend

This module is the primary native JavaFX desktop client for the Spring Boot REST API. JavaFX is a desktop toolkit and runs on a computer with a JDK; it does not run as a page inside a normal browser.

## Run on Windows

1. Start the Spring Boot API using a valid `MONGODB_URI` environment variable. The API must be reachable at `http://localhost:8080` unless you configure another URL.
2. Open PowerShell in the repository root and run:

   ```powershell
   $env:LIBRARY_API_URL = 'http://localhost:8080'
   .\desktop-frontend\start.ps1
   ```

   For an already deployed API, set `LIBRARY_API_URL` to its base URL before running the script. The script builds in the Windows temporary folder to avoid Maven write problems in OneDrive-synced folders.
3. Sign in with an account that exists in the backend. The app does not embed account passwords.

The first launch downloads Maven and JavaFX dependencies. JDK 21 or later is required.

## Screens

- Student: dashboard totals, browse books, OCR scanning from camera or image to issue/return books, submit fines, and review/edit the complete student profile.
- Administration: dashboard totals, member list, a separate pending-registration approval queue, book catalogue, OCR scanning from camera or image, issue/return desk, fines, and live circulation reports.
- Super Admin: dashboard totals, registered-student status list, Administration account management, and API/system connection status.
- All roles: account name and password settings, sign out, and an account-specific profile photo saved on the current computer.

Data actions call the existing `/api/...` endpoints. Dashboard tables and report totals are refreshed from the API; the client does not keep a separate database. Profile photos are local to the current computer and are not uploaded to MongoDB or shared with other devices.

The OCR scanner can capture a photo from the default camera or read a selected image. It matches recognized book IDs or title text against the live catalogue; verify the suggested book ID before issuing or returning. If the camera is unavailable, select an image or enter the ID manually. Windows may require the Microsoft Visual C++ v14 Redistributable for Tess4J's OCR native library.

## Student registration

The JavaFX registration page follows the BBAU blue-and-white sign-in design and includes name, father's name, enrollment number, gender, department, course, year, semester, email, mobile, address, state, district, Aadhaar ID, password confirmation, and data-use consent. Do not enter real identity details when testing a public demo.

The registration API stores the student details in MongoDB's `students` collection. Passwords continue to be stored as salted hashes by the backend.

Java applets are obsolete and are not supported by modern browsers or current Java desktop runtimes; JavaFX is used for the runnable desktop frontend.

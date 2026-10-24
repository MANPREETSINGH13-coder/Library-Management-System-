# Database notes

The backend uses MongoDB Atlas. Configure:

- `MONGODB_URI`
- `MONGODB_DATABASE=bbau_library`

Collections are created and seeded by the backend startup flow. Passwords are stored as salted PBKDF2 hashes. Keep connection strings and Super Admin credentials in Render environment variables.
# Database notes

The backend uses MongoDB Atlas. Configure:

- `MONGODB_URI`
- `MONGODB_DATABASE=bbau_library`

Collections are created and seeded by the backend startup flow. Passwords are stored as salted PBKDF2 hashes. Keep connection strings and Super Admin credentials in Render environment variables.
## String storage

New application records store scalar values as strings, including numeric IDs, quantities, amounts and boolean flags. Accounts already store string fields. The REST API still returns numeric and boolean DTO properties so the UI retains arithmetic and conditional behavior.

Legacy numeric IDs/quantities and boolean flags are accepted on read. Existing Atlas records are not automatically migrated. Numeric counters and inventory increments use atomic MongoDB update pipelines with numeric conversion followed by string conversion. Multi-document issue/return operations are still not transactional.

Example new stored book:

```json
{"_id":"1001","title":"Introduction to Java","author":"Herbert Schildt","category":"Computer science","copies":"5","available":"5"}
```

## Add book

Administration can enter an optional positive numeric Book ID, title, author, category, total copies and available copies. Empty ID is auto-generated. Available defaults to total when omitted by the API and must be between 0 and total; total must be at least 1. Duplicate IDs, blank title/author/category, and invalid copy counts are rejected. Existing client requests without ID/available continue to work. Unavailable copies entered at creation do not create historical issue records automatically.

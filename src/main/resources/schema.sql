CREATE TABLE IF NOT EXISTS accounts (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    name TEXT NOT NULL,
    role_name TEXT NOT NULL,
    student_id TEXT NOT NULL DEFAULT '',
    password_hash TEXT NOT NULL,
    status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    student_id TEXT NOT NULL,
    email TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS students_email_lower_uq ON students (lower(email));
CREATE UNIQUE INDEX IF NOT EXISTS students_id_lower_uq ON students (lower(student_id));

CREATE TABLE IF NOT EXISTS administrators (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    staff_role TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS administrators_email_lower_uq ON administrators (lower(email));

CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    category TEXT NOT NULL,
    copies INTEGER NOT NULL CHECK (copies > 0),
    available INTEGER NOT NULL CHECK (available >= 0 AND available <= copies)
);

CREATE TABLE IF NOT EXISTS issued_books (
    id BIGINT PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id),
    student_id TEXT NOT NULL,
    due_date DATE NOT NULL,
    returned BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS issued_books_student_id_idx ON issued_books (lower(student_id));

CREATE TABLE IF NOT EXISTS fines (
    id BIGINT PRIMARY KEY,
    student_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    status TEXT NOT NULL CHECK (status IN ('DUE', 'SUBMITTED', 'PAID'))
);
CREATE INDEX IF NOT EXISTS fines_student_id_idx ON fines (lower(student_id));

CREATE TABLE IF NOT EXISTS library_counters (
    id TEXT PRIMARY KEY,
    value BIGINT NOT NULL
);

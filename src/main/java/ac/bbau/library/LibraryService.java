package ac.bbau.library;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {
    public enum AccountStatus { PENDING, ACTIVE, REJECTED }

    public record Student(long id, String name, String studentId, String email, AccountStatus status) {}
    public record Administrator(long id, String name, String email, String staffRole) {}
    public record Book(long id, String title, String author, String category, int copies, int available) {}
    public record Loan(long id, long bookId, String studentId, String dueDate, boolean returned) {}
    public record Fine(long id, String studentId, String reason, int amount, String status) {}

    private static final RowMapper<Student> STUDENT = (row, index) -> new Student(row.getLong("id"), row.getString("name"),
            row.getString("student_id"), row.getString("email"), AccountStatus.valueOf(row.getString("status")));
    private static final RowMapper<Administrator> ADMINISTRATOR = (row, index) -> new Administrator(row.getLong("id"),
            row.getString("name"), row.getString("email"), row.getString("staff_role"));
    private static final RowMapper<Book> BOOK = (row, index) -> new Book(row.getLong("id"), row.getString("title"),
            row.getString("author"), row.getString("category"), row.getInt("copies"), row.getInt("available"));
    private static final RowMapper<Loan> LOAN = (row, index) -> new Loan(row.getLong("id"), row.getLong("book_id"),
            row.getString("student_id"), row.getString("due_date"), row.getBoolean("returned"));
    private static final RowMapper<Fine> FINE = (row, index) -> new Fine(row.getLong("id"), row.getString("student_id"),
            row.getString("reason"), row.getInt("amount"), row.getString("status"));

    private final JdbcTemplate jdbc;

    public LibraryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        seedDemoData();
    }

    private void seedDemoData() {
        jdbc.update("INSERT INTO library_counters (id, value) VALUES ('library', 1000) ON CONFLICT (id) DO NOTHING");
        jdbc.update("INSERT INTO administrators (id, name, email, staff_role) VALUES (1, 'Dr. Meera Joshi', 'meera.joshi@bbau.ac.in', 'Librarian') ON CONFLICT (id) DO NOTHING");
        seedStudent(201, "Manpreet Singh", "STU-2024-018", "manpreet@bbau.ac.in", AccountStatus.ACTIVE);
        seedStudent(202, "Milan Kumar", "STU-2023-104", "milan@bbau.ac.in", AccountStatus.ACTIVE);
        seedStudent(203, "Priya Sharma", "STU-2025-027", "priya@bbau.ac.in", AccountStatus.PENDING);
        if (count("books") == 0) {
            jdbc.update("INSERT INTO books (id, title, author, category, copies, available) VALUES (101, 'The Discovery of India', 'Jawaharlal Nehru', 'History', 8, 5), (102, 'Wings of Fire', 'A. P. J. Abdul Kalam', 'Biography', 12, 3), (103, 'The God of Small Things', 'Arundhati Roy', 'Fiction', 6, 0), (104, 'Introduction to Algorithms', 'Thomas H. Cormen', 'Computer science', 5, 2), (105, 'The White Tiger', 'Aravind Adiga', 'Fiction', 7, 1)");
        }
        if (count("issued_books") == 0) {
            jdbc.update("INSERT INTO issued_books (id, book_id, student_id, due_date, returned) VALUES (301, 103, 'STU-2024-018', DATE '2026-10-16', FALSE), (302, 102, 'STU-2023-104', DATE '2026-10-17', FALSE)");
        }
        if (count("fines") == 0) jdbc.update("INSERT INTO fines (id, student_id, reason, amount, status) VALUES (401, 'STU-2024-018', 'Late return', 50, 'DUE')");
    }

    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    private void seedStudent(long id, String name, String studentId, String email, AccountStatus status) {
        jdbc.update("INSERT INTO students (id, name, student_id, email, status) VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                id, name, studentId, email, status.name());
    }

    private long nextId() {
        return jdbc.queryForObject("UPDATE library_counters SET value = value + 1 WHERE id = 'library' RETURNING value", Long.class);
    }

    public synchronized Student registerStudent(String name, String studentId, String email) {
        boolean duplicate = Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM students WHERE lower(email) = lower(?) OR lower(student_id) = lower(?))",
                Boolean.class, email, studentId));
        if (duplicate) throw new IllegalArgumentException("A student with this email or student ID already exists.");
        long id = nextId();
        jdbc.update("INSERT INTO students (id, name, student_id, email, status) VALUES (?, ?, ?, ?, 'PENDING')", id, name, studentId, email);
        return jdbc.queryForObject("SELECT * FROM students WHERE id = ?", STUDENT, id);
    }

    public synchronized Student reviewStudent(long id, boolean approve) {
        int changed = jdbc.update("UPDATE students SET status = ? WHERE id = ? AND status = 'PENDING'",
                approve ? "ACTIVE" : "REJECTED", id);
        if (changed == 0) {
            if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM students WHERE id = ?)", Boolean.class, id))
                throw new IllegalStateException("This request has already been reviewed.");
            throw new NoSuchElementException("Student not found.");
        }
        return jdbc.queryForObject("SELECT * FROM students WHERE id = ?", STUDENT, id);
    }

    public synchronized Administrator createAdministrator(String name, String email, String staffRole) {
        boolean duplicate = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM administrators WHERE lower(email) = lower(?))", Boolean.class, email));
        if (duplicate) throw new IllegalArgumentException("An administrator with this email already exists.");
        long id = nextId();
        jdbc.update("INSERT INTO administrators (id, name, email, staff_role) VALUES (?, ?, ?, ?)", id, name, email, staffRole);
        return jdbc.queryForObject("SELECT * FROM administrators WHERE id = ?", ADMINISTRATOR, id);
    }

    public synchronized Book addBook(String title, String author, String category, int copies) {
        if (copies < 1) throw new IllegalArgumentException("At least one copy is required.");
        long id = nextId();
        jdbc.update("INSERT INTO books (id, title, author, category, copies, available) VALUES (?, ?, ?, ?, ?, ?)", id, title, author, category, copies, copies);
        return jdbc.queryForObject("SELECT * FROM books WHERE id = ?", BOOK, id);
    }

    public synchronized void deleteBook(String title) {
        Book book = jdbc.query("SELECT * FROM books WHERE lower(title) = lower(?)", BOOK, title).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Book not found."));
        boolean activeIssue = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM issued_books WHERE book_id = ? AND returned = FALSE)", Boolean.class, book.id()));
        if (activeIssue) throw new IllegalStateException("Return all issued copies before deleting this title.");
        jdbc.update("DELETE FROM books WHERE id = ?", book.id());
    }

    public synchronized Fine addFine(String studentId, String reason, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Fine amount must be at least ₹1.");
        long id = nextId();
        jdbc.update("INSERT INTO fines (id, student_id, reason, amount, status) VALUES (?, ?, ?, ?, 'DUE')", id, studentId, reason, amount);
        return jdbc.queryForObject("SELECT * FROM fines WHERE id = ?", FINE, id);
    }

    public synchronized Fine submitFine(long id, String studentId) {
        int changed = jdbc.update("UPDATE fines SET status = 'SUBMITTED' WHERE id = ? AND student_id = ? AND status = 'DUE'", id, studentId);
        if (changed == 0) {
            Fine fine = jdbc.query("SELECT * FROM fines WHERE id = ? AND student_id = ?", FINE, id, studentId).stream().findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Fine not found."));
            throw new IllegalStateException(fine.status().equals("DUE") ? "Fine submission could not be saved." : "Only due fines can be submitted.");
        }
        return jdbc.queryForObject("SELECT * FROM fines WHERE id = ?", FINE, id);
    }

    public synchronized Fine confirmFine(long id) {
        int changed = jdbc.update("UPDATE fines SET status = 'PAID' WHERE id = ? AND status = 'SUBMITTED'", id);
        if (changed == 0) {
            if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM fines WHERE id = ?)", Boolean.class, id))
                throw new IllegalStateException("This fine has no payment submission to confirm.");
            throw new NoSuchElementException("Fine not found.");
        }
        return jdbc.queryForObject("SELECT * FROM fines WHERE id = ?", FINE, id);
    }

    @Transactional
    public synchronized Loan issueBook(long bookId, String studentId, String dueDate) {
        int changed = jdbc.update("UPDATE books SET available = available - 1 WHERE id = ? AND available > 0", bookId);
        if (changed == 0) {
            if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM books WHERE id = ?)", Boolean.class, bookId)) throw new IllegalStateException("No copies are available.");
            throw new NoSuchElementException("Book not found.");
        }
        long id = nextId();
        jdbc.update("INSERT INTO issued_books (id, book_id, student_id, due_date, returned) VALUES (?, ?, ?, CAST(? AS DATE), FALSE)", id, bookId, studentId, dueDate);
        return jdbc.queryForObject("SELECT id, book_id, student_id, to_char(due_date, 'YYYY-MM-DD') AS due_date, returned FROM issued_books WHERE id = ?", LOAN, id);
    }

    @Transactional
    public synchronized Loan returnBook(long loanId) {
        int changed = jdbc.update("UPDATE issued_books SET returned = TRUE WHERE id = ? AND returned = FALSE", loanId);
        if (changed == 0) {
            if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM issued_books WHERE id = ?)", Boolean.class, loanId)) throw new IllegalStateException("This book has already been returned.");
            throw new NoSuchElementException("Loan not found.");
        }
        Loan loan = jdbc.queryForObject("SELECT id, book_id, student_id, to_char(due_date, 'YYYY-MM-DD') AS due_date, returned FROM issued_books WHERE id = ?", LOAN, loanId);
        jdbc.update("UPDATE books SET available = LEAST(copies, available + 1) WHERE id = ?", loan.bookId());
        return loan;
    }

    public synchronized Loan returnBook(long loanId, String studentId) {
        Loan loan = jdbc.query("SELECT id, book_id, student_id, to_char(due_date, 'YYYY-MM-DD') AS due_date, returned FROM issued_books WHERE id = ?", LOAN, loanId)
                .stream().findFirst().orElseThrow(() -> new NoSuchElementException("Loan not found."));
        if (!loan.studentId().equalsIgnoreCase(studentId)) throw new NoSuchElementException("Loan not found.");
        return returnBook(loanId);
    }

    public List<Student> students() { return jdbc.query("SELECT * FROM students ORDER BY id", STUDENT); }
    public List<Student> pendingStudents() { return jdbc.query("SELECT * FROM students WHERE status = 'PENDING' ORDER BY id", STUDENT); }
    public List<Administrator> administrators() { return jdbc.query("SELECT * FROM administrators ORDER BY id", ADMINISTRATOR); }
    public List<Book> books() { return jdbc.query("SELECT * FROM books ORDER BY id", BOOK); }
    public List<Loan> loans() { return jdbc.query("SELECT id, book_id, student_id, to_char(due_date, 'YYYY-MM-DD') AS due_date, returned FROM issued_books ORDER BY id", LOAN); }
    public List<Loan> loansForStudent(String studentId) { return jdbc.query("SELECT id, book_id, student_id, to_char(due_date, 'YYYY-MM-DD') AS due_date, returned FROM issued_books WHERE lower(student_id) = lower(?) ORDER BY id", LOAN, studentId); }
    public List<Fine> fines() { return jdbc.query("SELECT * FROM fines ORDER BY id", FINE); }
    public List<Fine> finesForStudent(String studentId) { return jdbc.query("SELECT * FROM fines WHERE lower(student_id) = lower(?) ORDER BY id", FINE, studentId); }
}

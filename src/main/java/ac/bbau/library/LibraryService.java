package ac.bbau.library;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {
    public enum AccountStatus { PENDING, ACTIVE, REJECTED }
    public record Student(long id, String name, String studentId, String email, AccountStatus status) {}
    public record Administrator(long id, String name, String email, String staffRole) {}
    public record Book(long id, String title, String author, String category, int copies, int available) {}
    public record Loan(long id, long bookId, String studentId, String dueDate, boolean returned) {}
    public record Fine(long id, String studentId, String reason, int amount, String status) {}

    private final AtomicLong ids = new AtomicLong(1000);
    private final Map<Long, Student> students = new LinkedHashMap<>();
    private final Map<Long, Administrator> administrators = new LinkedHashMap<>();
    private final Map<Long, Book> books = new LinkedHashMap<>();
    private final Map<Long, Loan> loans = new LinkedHashMap<>();
    private final Map<Long, Fine> fines = new LinkedHashMap<>();

    public LibraryService() {
        administrators.put(1L, new Administrator(1, "Dr. Meera Joshi", "meera.joshi@bbau.ac.in", "Librarian"));
        students.put(201L, new Student(201, "Manpreet Singh", "STU-2024-018", "manpreet@bbau.ac.in", AccountStatus.ACTIVE));
        students.put(202L, new Student(202, "Milan Kumar", "STU-2023-104", "milan@bbau.ac.in", AccountStatus.ACTIVE));
        students.put(203L, new Student(203, "Priya Sharma", "STU-2025-027", "priya@bbau.ac.in", AccountStatus.PENDING));
        books.put(101L, new Book(101, "The Discovery of India", "Jawaharlal Nehru", "History", 8, 5));
        books.put(102L, new Book(102, "Wings of Fire", "A. P. J. Abdul Kalam", "Biography", 12, 3));
        books.put(103L, new Book(103, "The God of Small Things", "Arundhati Roy", "Fiction", 6, 0));
        books.put(104L, new Book(104, "Introduction to Algorithms", "Thomas H. Cormen", "Computer science", 5, 2));
        books.put(105L, new Book(105, "The White Tiger", "Aravind Adiga", "Fiction", 7, 1));
        loans.put(301L, new Loan(301, 103, "STU-2024-018", "2026-10-16", false));
        loans.put(302L, new Loan(302, 102, "STU-2023-104", "2026-10-17", false));
        fines.put(401L, new Fine(401, "STU-2024-018", "Late return", 50, "DUE"));
    }

    public synchronized Student registerStudent(String name, String studentId, String email) {
        if (students.values().stream().anyMatch(s -> s.email().equalsIgnoreCase(email) || s.studentId().equalsIgnoreCase(studentId)))
            throw new IllegalArgumentException("A student with this email or student ID already exists.");
        Student student = new Student(ids.incrementAndGet(), name, studentId, email, AccountStatus.PENDING);
        students.put(student.id(), student);
        return student;
    }

    public synchronized Student reviewStudent(long id, boolean approve) {
        Student existing = Optional.ofNullable(students.get(id)).orElseThrow(() -> new NoSuchElementException("Student not found."));
        if (existing.status() != AccountStatus.PENDING) throw new IllegalStateException("This request has already been reviewed.");
        Student reviewed = new Student(existing.id(), existing.name(), existing.studentId(), existing.email(), approve ? AccountStatus.ACTIVE : AccountStatus.REJECTED);
        students.put(id, reviewed);
        return reviewed;
    }

    public synchronized Administrator createAdministrator(String name, String email, String staffRole) {
        if (administrators.values().stream().anyMatch(a -> a.email().equalsIgnoreCase(email)))
            throw new IllegalArgumentException("An administrator with this email already exists.");
        Administrator administrator = new Administrator(ids.incrementAndGet(), name, email, staffRole);
        administrators.put(administrator.id(), administrator);
        return administrator;
    }

    public synchronized Book addBook(String title, String author, String category, int copies) {
        if (copies < 1) throw new IllegalArgumentException("At least one copy is required.");
        Book book = new Book(ids.incrementAndGet(), title, author, category, copies, copies);
        books.put(book.id(), book);
        return book;
    }

    public synchronized void deleteBook(String title) {
        Book book = books.values().stream().filter(b -> b.title().equalsIgnoreCase(title)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Book not found."));
        if (loans.values().stream().anyMatch(l -> l.bookId() == book.id() && !l.returned()))
            throw new IllegalStateException("Return all borrowed copies before deleting this title.");
        books.remove(book.id());
    }

    public synchronized Fine addFine(String studentId, String reason, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Fine amount must be at least ₹1.");
        Fine fine = new Fine(ids.incrementAndGet(), studentId, reason, amount, "DUE");
        fines.put(fine.id(), fine);
        return fine;
    }

    public synchronized Fine submitFine(long id, String studentId) {
        Fine fine = Optional.ofNullable(fines.get(id)).orElseThrow(() -> new NoSuchElementException("Fine not found."));
        if (!fine.studentId().equals(studentId)) throw new NoSuchElementException("Fine not found.");
        if (!fine.status().equals("DUE")) throw new IllegalStateException("Only due fines can be submitted.");
        Fine submitted = new Fine(fine.id(), fine.studentId(), fine.reason(), fine.amount(), "SUBMITTED");
        fines.put(id, submitted);
        return submitted;
    }

    public synchronized Fine confirmFine(long id) {
        Fine fine = Optional.ofNullable(fines.get(id)).orElseThrow(() -> new NoSuchElementException("Fine not found."));
        if (!fine.status().equals("SUBMITTED")) throw new IllegalStateException("This fine has no payment submission to confirm.");
        Fine paid = new Fine(fine.id(), fine.studentId(), fine.reason(), fine.amount(), "PAID");
        fines.put(id, paid);
        return paid;
    }

    public synchronized Loan issueBook(long bookId, String studentId, String dueDate) {
        Book book = Optional.ofNullable(books.get(bookId)).orElseThrow(() -> new NoSuchElementException("Book not found."));
        if (book.available() < 1) throw new IllegalStateException("No copies are available.");
        books.put(bookId, new Book(book.id(), book.title(), book.author(), book.category(), book.copies(), book.available() - 1));
        Loan loan = new Loan(ids.incrementAndGet(), bookId, studentId, dueDate, false);
        loans.put(loan.id(), loan);
        return loan;
    }

    public synchronized Loan returnBook(long loanId) {
        Loan loan = Optional.ofNullable(loans.get(loanId)).orElseThrow(() -> new NoSuchElementException("Loan not found."));
        if (loan.returned()) throw new IllegalStateException("This book has already been returned.");
        Book book = books.get(loan.bookId());
        books.put(book.id(), new Book(book.id(), book.title(), book.author(), book.category(), book.copies(), book.available() + 1));
        Loan returned = new Loan(loan.id(), loan.bookId(), loan.studentId(), loan.dueDate(), true);
        loans.put(loanId, returned);
        return returned;
    }

    public synchronized Loan returnBook(long loanId, String studentId) {
        Loan loan = Optional.ofNullable(loans.get(loanId)).orElseThrow(() -> new NoSuchElementException("Loan not found."));
        if (!loan.studentId().equalsIgnoreCase(studentId)) throw new NoSuchElementException("Loan not found.");
        return returnBook(loanId);
    }

    public synchronized List<Student> students() { return List.copyOf(students.values()); }
    public synchronized List<Student> pendingStudents() { return students.values().stream().filter(s -> s.status() == AccountStatus.PENDING).toList(); }
    public synchronized List<Administrator> administrators() { return List.copyOf(administrators.values()); }
    public synchronized List<Book> books() { return List.copyOf(books.values()); }
    public synchronized List<Loan> loans() { return List.copyOf(loans.values()); }
    public synchronized List<Loan> loansForStudent(String studentId) { return loans.values().stream().filter(l -> l.studentId().equalsIgnoreCase(studentId)).toList(); }
    public synchronized List<Fine> fines() { return List.copyOf(fines.values()); }
    public synchronized List<Fine> finesForStudent(String studentId) { return fines.values().stream().filter(f -> f.studentId().equals(studentId)).toList(); }
}

package ac.bbau.library;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {
    public enum AccountStatus { PENDING, ACTIVE, REJECTED }

    @Document("students")
    public record Student(@Id long id, String name, String studentId, String email, AccountStatus status) {}
    @Document("administrators")
    public record Administrator(@Id long id, String name, String email, String staffRole) {}
    @Document("books")
    public record Book(@Id long id, String title, String author, String category, int copies, int available) {}
    @Document("issued_books")
    public record Loan(@Id long id, long bookId, String studentId, String dueDate, boolean returned) {}
    @Document("fines")
    public record Fine(@Id long id, String studentId, String reason, int amount, String status) {}

    @Document("library_counters")
    public static class Counter {
        @Id private String id;
        private long value;
        public Counter() {}
        public Counter(String id, long value) { this.id = id; this.value = value; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
    }

    private final MongoTemplate mongo;

    public LibraryService(MongoTemplate mongo) {
        this.mongo = mongo;
        seedDemoData();
    }

    private void seedDemoData() {
        if (!mongo.exists(Query.query(Criteria.where("_id").is(1L)), Administrator.class))
            mongo.save(new Administrator(1, "Dr. Meera Joshi", "meera.joshi@bbau.ac.in", "Librarian"));
        seedStudent(201, "Manpreet Singh", "STU-2024-018", "manpreet@bbau.ac.in", AccountStatus.ACTIVE);
        seedStudent(202, "Milan Kumar", "STU-2023-104", "milan@bbau.ac.in", AccountStatus.ACTIVE);
        seedStudent(203, "Priya Sharma", "STU-2025-027", "priya@bbau.ac.in", AccountStatus.PENDING);
        if (mongo.count(new Query(), Book.class) == 0) {
            mongo.save(new Book(101, "The Discovery of India", "Jawaharlal Nehru", "History", 8, 5));
            mongo.save(new Book(102, "Wings of Fire", "A. P. J. Abdul Kalam", "Biography", 12, 3));
            mongo.save(new Book(103, "The God of Small Things", "Arundhati Roy", "Fiction", 6, 0));
            mongo.save(new Book(104, "Introduction to Algorithms", "Thomas H. Cormen", "Computer science", 5, 2));
            mongo.save(new Book(105, "The White Tiger", "Aravind Adiga", "Fiction", 7, 1));
        }
        if (mongo.count(new Query(), Loan.class) == 0) {
            mongo.save(new Loan(301, 103, "STU-2024-018", "2026-10-16", false));
            mongo.save(new Loan(302, 102, "STU-2023-104", "2026-10-17", false));
        }
        if (mongo.count(new Query(), Fine.class) == 0)
            mongo.save(new Fine(401, "STU-2024-018", "Late return", 50, "DUE"));
        mongo.upsert(Query.query(Criteria.where("_id").is("library")), new Update().max("value", 1000), Counter.class);
    }

    private void seedStudent(long id, String name, String studentId, String email, AccountStatus status) {
        if (!mongo.exists(Query.query(Criteria.where("_id").is(id)), Student.class))
            mongo.save(new Student(id, name, studentId, email, status));
    }

    private long nextId() {
        Counter counter = mongo.findAndModify(Query.query(Criteria.where("_id").is("library")), new Update().inc("value", 1),
                FindAndModifyOptions.options().returnNew(true), Counter.class);
        return Optional.ofNullable(counter).orElseThrow(() -> new IllegalStateException("Could not allocate a library record ID.")).getValue();
    }

    public synchronized Student registerStudent(String name, String studentId, String email) {
        boolean duplicate = mongo.exists(Query.query(new Criteria().orOperator(
                Criteria.where("email").regex("^" + java.util.regex.Pattern.quote(email) + "$", "i"),
                Criteria.where("studentId").regex("^" + java.util.regex.Pattern.quote(studentId) + "$", "i"))), Student.class);
        if (duplicate) throw new IllegalArgumentException("A student with this email or student ID already exists.");
        Student student = new Student(nextId(), name, studentId, email, AccountStatus.PENDING);
        return mongo.save(student);
    }

    public synchronized Student reviewStudent(long id, boolean approve) {
        Student existing = Optional.ofNullable(mongo.findById(id, Student.class)).orElseThrow(() -> new NoSuchElementException("Student not found."));
        if (existing.status() != AccountStatus.PENDING) throw new IllegalStateException("This request has already been reviewed.");
        return mongo.save(new Student(existing.id(), existing.name(), existing.studentId(), existing.email(), approve ? AccountStatus.ACTIVE : AccountStatus.REJECTED));
    }

    public synchronized Administrator createAdministrator(String name, String email, String staffRole) {
        if (mongo.exists(Query.query(Criteria.where("email").regex("^" + java.util.regex.Pattern.quote(email) + "$", "i")), Administrator.class))
            throw new IllegalArgumentException("An administrator with this email already exists.");
        return mongo.save(new Administrator(nextId(), name, email, staffRole));
    }

    public synchronized Book addBook(String title, String author, String category, int copies) {
        if (copies < 1) throw new IllegalArgumentException("At least one copy is required.");
        return mongo.save(new Book(nextId(), title, author, category, copies, copies));
    }

    public synchronized void deleteBook(String title) {
        Book book = mongo.findOne(Query.query(Criteria.where("title").regex("^" + java.util.regex.Pattern.quote(title) + "$", "i")), Book.class);
        if (book == null) throw new NoSuchElementException("Book not found.");
        if (mongo.exists(Query.query(Criteria.where("bookId").is(book.id()).and("returned").is(false)), Loan.class))
            throw new IllegalStateException("Return all issued copies before deleting this title.");
        mongo.remove(Query.query(Criteria.where("_id").is(book.id())), Book.class);
    }

    public synchronized Fine addFine(String studentId, String reason, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Fine amount must be at least ₹1.");
        return mongo.save(new Fine(nextId(), studentId, reason, amount, "DUE"));
    }

    public synchronized Fine submitFine(long id, String studentId) {
        Fine fine = Optional.ofNullable(mongo.findById(id, Fine.class)).orElseThrow(() -> new NoSuchElementException("Fine not found."));
        if (!fine.studentId().equals(studentId)) throw new NoSuchElementException("Fine not found.");
        if (!fine.status().equals("DUE")) throw new IllegalStateException("Only due fines can be submitted.");
        return mongo.save(new Fine(fine.id(), fine.studentId(), fine.reason(), fine.amount(), "SUBMITTED"));
    }

    public synchronized Fine confirmFine(long id) {
        Fine fine = Optional.ofNullable(mongo.findById(id, Fine.class)).orElseThrow(() -> new NoSuchElementException("Fine not found."));
        if (!fine.status().equals("SUBMITTED")) throw new IllegalStateException("This fine has no payment submission to confirm.");
        return mongo.save(new Fine(fine.id(), fine.studentId(), fine.reason(), fine.amount(), "PAID"));
    }

    public synchronized Loan issueBook(long bookId, String studentId, String dueDate) {
        Book book = mongo.findAndModify(Query.query(Criteria.where("_id").is(bookId).and("available").gt(0)),
                new Update().inc("available", -1), FindAndModifyOptions.options().returnNew(true), Book.class);
        if (book == null) {
            if (mongo.exists(Query.query(Criteria.where("_id").is(bookId)), Book.class)) throw new IllegalStateException("No copies are available.");
            throw new NoSuchElementException("Book not found.");
        }
        return mongo.save(new Loan(nextId(), bookId, studentId, dueDate, false));
    }

    public synchronized Loan returnBook(long loanId) {
        Loan loan = mongo.findAndModify(Query.query(Criteria.where("_id").is(loanId).and("returned").is(false)),
                new Update().set("returned", true), FindAndModifyOptions.options().returnNew(true), Loan.class);
        if (loan == null) {
            if (mongo.exists(Query.query(Criteria.where("_id").is(loanId)), Loan.class)) throw new IllegalStateException("This book has already been returned.");
            throw new NoSuchElementException("Loan not found.");
        }
        mongo.updateFirst(Query.query(Criteria.where("_id").is(loan.bookId())), new Update().inc("available", 1), Book.class);
        return loan;
    }

    public synchronized Loan returnBook(long loanId, String studentId) {
        Loan loan = Optional.ofNullable(mongo.findById(loanId, Loan.class)).orElseThrow(() -> new NoSuchElementException("Loan not found."));
        if (!loan.studentId().equalsIgnoreCase(studentId)) throw new NoSuchElementException("Loan not found.");
        return returnBook(loanId);
    }

    public List<Student> students() { return mongo.findAll(Student.class); }
    public List<Student> pendingStudents() { return mongo.find(Query.query(Criteria.where("status").is(AccountStatus.PENDING)), Student.class); }
    public List<Administrator> administrators() { return mongo.findAll(Administrator.class); }
    public List<Book> books() { return mongo.findAll(Book.class); }
    public List<Loan> loans() { return mongo.findAll(Loan.class); }
    public List<Loan> loansForStudent(String studentId) { return mongo.find(Query.query(Criteria.where("studentId").regex("^" + java.util.regex.Pattern.quote(studentId) + "$", "i")), Loan.class); }
    public List<Fine> fines() { return mongo.findAll(Fine.class); }
    public List<Fine> finesForStudent(String studentId) { return mongo.find(Query.query(Criteria.where("studentId").is(studentId)), Fine.class); }
}

package ac.bbau.library;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class LibraryController {
    private final LibraryService library;
    private final AuthService auth;
    public LibraryController(LibraryService library, AuthService auth) { this.library = library; this.auth = auth; }

    @PostMapping("/auth/login")
    public AuthService.Session login(@RequestBody Login request) { return auth.login(request.email(), request.password(), request.role()); }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) { auth.logout(authorization); }

    @PostMapping("/students/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Student register(@RequestBody Registration request) {
        auth.registerStudent(request.name(), request.email(), request.studentId(), request.password());
        try {
            return library.registerStudent(request.name(), request.studentId(), request.email());
        } catch (RuntimeException exception) {
            auth.cancelStudentRegistration(request.email());
            throw exception;
        }
    }

    @GetMapping("/admin/students")
    public List<LibraryService.Student> students(@RequestHeader("Authorization") String authorization, @RequestParam(defaultValue = "false") boolean pendingOnly) {
        auth.requireRole(authorization, "Administration");
        return pendingOnly ? library.pendingStudents() : library.students();
    }

    @PostMapping("/admin/students/{id}/review")
    public LibraryService.Student review(@RequestHeader("Authorization") String authorization, @PathVariable long id, @RequestBody Review request) {
        auth.requireRole(authorization, "Administration");
        LibraryService.Student student = library.reviewStudent(id, request.approve());
        auth.reviewStudent(student.email(), request.approve());
        return student;
    }

    @GetMapping("/super-admin/administrators")
    public List<LibraryService.Administrator> administrators(@RequestHeader("Authorization") String authorization) {
        auth.requireRole(authorization, "Super Admin");
        return library.administrators();
    }

    @PostMapping("/super-admin/administrators")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Administrator createAdministrator(@RequestHeader("Authorization") String authorization, @RequestBody NewAdministrator request) {
        auth.requireRole(authorization, "Super Admin");
        String staffRole = List.of("Librarian", "Library staff").contains(request.staffRole()) ? request.staffRole() : "Librarian";
        LibraryService.Administrator administrator = library.createAdministrator(request.name(), request.email(), staffRole);
        auth.createAdministrator(request.name(), request.email(), request.password());
        return administrator;
    }

    @GetMapping("/books")
    public List<LibraryService.Book> books() { return library.books(); }

    @PostMapping("/admin/books")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Book addBook(@RequestHeader("Authorization") String authorization, @RequestBody NewBook request) {
        auth.requireRole(authorization, "Administration");
        return library.addBook(request.title(), request.author(), request.category(), request.copies());
    }

    @DeleteMapping("/admin/books")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@RequestHeader("Authorization") String authorization, @RequestParam String title) {
        auth.requireRole(authorization, "Administration");
        library.deleteBook(title);
    }

    @GetMapping("/admin/fines")
    public List<LibraryService.Fine> fines(@RequestHeader("Authorization") String authorization) {
        auth.requireRole(authorization, "Administration");
        return library.fines();
    }

    @PostMapping("/admin/fines")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Fine addFine(@RequestHeader("Authorization") String authorization, @RequestBody NewFine request) {
        auth.requireRole(authorization, "Administration");
        return library.addFine(request.studentId(), request.reason(), request.amount());
    }

    @GetMapping("/student/fines")
    public List<LibraryService.Fine> studentFines(@RequestHeader("Authorization") String authorization, @RequestParam String studentId) {
        String ownId = auth.requireStudentId(authorization);
        if (!ownId.equalsIgnoreCase(studentId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own fines.");
        return library.finesForStudent(ownId);
    }

    @PostMapping("/student/fines/{id}/submit")
    public LibraryService.Fine submitFine(@RequestHeader("Authorization") String authorization, @PathVariable long id, @RequestBody StudentFine request) {
        String ownId = auth.requireStudentId(authorization);
        return library.submitFine(id, ownId);
    }

    @PostMapping("/admin/fines/{id}/confirm")
    public LibraryService.Fine confirmFine(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        auth.requireRole(authorization, "Administration");
        return library.confirmFine(id);
    }

    @PostMapping("/admin/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Loan issue(@RequestHeader("Authorization") String authorization, @RequestBody NewLoan request) {
        auth.requireRole(authorization, "Administration");
        return library.issueBook(request.bookId(), request.studentId(), request.dueDate());
    }

    @PostMapping("/admin/loans/{id}/return")
    public LibraryService.Loan returnBook(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        auth.requireRole(authorization, "Administration");
        return library.returnBook(id);
    }

    @GetMapping("/admin/loans")
    public List<LibraryService.Loan> loans(@RequestHeader("Authorization") String authorization) {
        auth.requireRole(authorization, "Administration");
        return library.loans();
    }

    @GetMapping("/student/loans")
    public List<LibraryService.Loan> studentLoans(@RequestHeader("Authorization") String authorization, @RequestParam String studentId) {
        String ownId = auth.requireStudentId(authorization);
        if (!ownId.equalsIgnoreCase(studentId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own loans.");
        return library.loansForStudent(ownId);
    }

    @PostMapping("/student/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryService.Loan studentIssue(@RequestHeader("Authorization") String authorization, @RequestBody NewLoan request) {
        String ownId = auth.requireStudentId(authorization);
        return library.issueBook(request.bookId(), ownId, request.dueDate());
    }

    @PostMapping("/student/loans/{id}/return")
    public LibraryService.Loan studentReturn(@RequestHeader("Authorization") String authorization, @PathVariable long id) {
        String ownId = auth.requireStudentId(authorization);
        return library.returnBook(id, ownId);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(RuntimeException exception) { return Map.of("error", exception.getMessage()); }

    public record Login(String email, String password, String role) {}
    public record Registration(String name, String studentId, String email, String password) {}
    public record Review(boolean approve) {}
    public record NewAdministrator(String name, String email, String staffRole, String password) {}
    public record NewBook(String title, String author, String category, int copies) {}
    public record NewLoan(long bookId, String studentId, String dueDate) {}
    public record NewFine(String studentId, String reason, int amount) {}
    public record StudentFine(String studentId) {}
}

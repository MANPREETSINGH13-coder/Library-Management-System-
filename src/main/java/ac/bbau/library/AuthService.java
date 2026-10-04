package ac.bbau.library;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    public record Session(String token, String role, String name, String studentId) {}
    public record Profile(String email, String name, String role, String studentId) {}

    public record Account(String id, String email, String name, String role, String studentId, String passwordHash, String status) {}

    private final AccountRepository accountRepository;
    private final Map<String, Account> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public AuthService(AccountRepository accountRepository,
            @Value("${SUPER_ADMIN_EMAIL:superadmin@bbau.ac.in}") String superAdminEmail,
            @Value("${SUPER_ADMIN_PASSWORD:Admin@123}") String superAdminPassword) {
        this.accountRepository = accountRepository;
        configureSuperAdmin(superAdminEmail, superAdminPassword);
        addDemo("meera.joshi@bbau.ac.in", "Dr. Meera Joshi", "Administration", "", "Library@123", "ACTIVE");
        addDemo("manpreet@bbau.ac.in", "Manpreet Singh", "Student", "STU-2024-018", "Student@123", "ACTIVE");
        addDemo("milan@bbau.ac.in", "Milan Kumar", "Student", "STU-2023-104", "Student@123", "ACTIVE");
        addDemo("priya@bbau.ac.in", "Priya Sharma", "Student", "STU-2025-027", "Student@123", "PENDING");
    }

    private void configureSuperAdmin(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.length() < 8)
            throw new IllegalStateException("SUPER_ADMIN_EMAIL and a SUPER_ADMIN_PASSWORD of at least 8 characters are required.");

        String id = key(email);
        Account existing = accountRepository.findById(id).orElse(null);
        if (existing != null && !"Super Admin".equals(existing.role()))
            throw new IllegalStateException("SUPER_ADMIN_EMAIL is already used by a non-Super Admin account.");

        String legacyId = key("superadmin@bbau.ac.in");
        if (!id.equals(legacyId)) {
            Account legacy = accountRepository.findById(legacyId).orElse(null);
            if (legacy != null && "Super Admin".equals(legacy.role())) accountRepository.deleteById(legacyId);
        }

        accountRepository.save(new Account(id, email.trim(), "Super Admin", "Super Admin", "", hash(password), "ACTIVE"));
    }

    private void addDemo(String email, String name, String role, String studentId, String password, String status) {
        String id = key(email);
        if (!accountRepository.existsById(id)) accountRepository.save(new Account(id, email, name, role, studentId, hash(password), status));
    }

    public Session login(String email, String password, String requestedRole) {
        Account account = accountRepository.findById(key(email)).orElse(null);
        if (account == null || !matches(password, account.passwordHash()) || !account.role().equals(requestedRole))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email, password, or account type is incorrect.");
        if ("PENDING".equals(account.status()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your student registration is waiting for Administration approval.");
        if ("REJECTED".equals(account.status()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This student registration was not approved. Contact Administration.");
        String token = UUID.randomUUID().toString();
        sessions.put(token, account);
        return new Session(token, account.role(), account.name(), account.studentId());
    }

    public synchronized void registerStudent(String name, String email, String studentId, String password) {
        addAccount(name, email, "Student", studentId, password, "PENDING");
    }

    public synchronized void createAdministrator(String name, String email, String password) {
        addAccount(name, email, "Administration", "", password, "ACTIVE");
    }

    public synchronized void cancelStudentRegistration(String email) {
        String id = key(email);
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null && "Student".equals(account.role()) && "PENDING".equals(account.status())) accountRepository.deleteById(id);
    }

    public synchronized void reviewStudent(String email, boolean approved) {
        String id = key(email);
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null && "Student".equals(account.role())) {
            accountRepository.save(new Account(account.id(), account.email(), account.name(), account.role(), account.studentId(),
                    account.passwordHash(), approved ? "ACTIVE" : "REJECTED"));
        }
    }

    public String requireRole(String authorization, String expectedRole) {
        Account account = accountFrom(authorization);
        if (!expectedRole.equals(account.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action requires the " + expectedRole + " role.");
        return account.email();
    }

    public String requireStudentId(String authorization) {
        Account account = accountFrom(authorization);
        if (!"Student".equals(account.role()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action requires the Student role.");
        return account.studentId();
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) sessions.remove(authorization.substring(7).trim());
    }

    public Profile profile(String authorization) {
        Account account = accountFrom(authorization);
        return new Profile(account.email(), account.name(), account.role(), account.studentId());
    }

    public Profile updateProfile(String authorization, String name, String currentPassword, String newPassword) {
        String token = tokenFrom(authorization);
        Account account = accountFrom(authorization);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required.");
        String nextHash = account.passwordHash();
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) throw new IllegalArgumentException("New password must be at least 8 characters.");
            if (!matches(currentPassword, account.passwordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
            nextHash = hash(newPassword);
        }
        Account updated = new Account(account.id(), account.email(), name.trim(), account.role(), account.studentId(), nextHash, account.status());
        accountRepository.save(updated);
        sessions.put(token, updated);
        return new Profile(updated.email(), updated.name(), updated.role(), updated.studentId());
    }

    private Account accountFrom(String authorization) {
        String token = tokenFrom(authorization);
        Account account = sessions.get(token);
        if (account == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again.");
        return account;
    }

    private String tokenFrom(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        return authorization.substring(7).trim();
    }

    private void addAccount(String name, String email, String role, String studentId, String password, String status) {
        if (name == null || name.isBlank() || email == null || !email.contains("@") || password == null || password.length() < 8)
            throw new IllegalArgumentException("Name, valid email, and a password of at least 8 characters are required.");
        String id = key(email);
        if (accountRepository.existsById(id)) throw new IllegalArgumentException("An account with this email already exists.");
        accountRepository.save(new Account(id, email.trim(), name.trim(), role, studentId == null ? "" : studentId.trim(), hash(password), status));
    }

    private static String key(String email) { return email == null ? "" : email.trim().toLowerCase(); }

    private String hash(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(derived);
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        }
    }

    private boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        try {
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, expected.length * 8);
            byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return java.security.MessageDigest.isEqual(expected, actual);
        } catch (Exception exception) {
            return false;
        }
    }
}

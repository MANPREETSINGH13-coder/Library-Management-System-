package ac.bbau.library;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepository {
    private static final RowMapper<AuthService.Account> MAPPER = AccountRepository::mapAccount;
    private final JdbcTemplate jdbc;

    AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    boolean existsById(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM accounts WHERE id = ?)", Boolean.class, id));
    }

    Optional<AuthService.Account> findById(String id) {
        return jdbc.query("SELECT id, email, name, role_name, student_id, password_hash, status FROM accounts WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    AuthService.Account save(AuthService.Account account) {
        jdbc.update("""
                INSERT INTO accounts (id, email, name, role_name, student_id, password_hash, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, name = EXCLUDED.name,
                    role_name = EXCLUDED.role_name, student_id = EXCLUDED.student_id,
                    password_hash = EXCLUDED.password_hash, status = EXCLUDED.status
                """, account.id(), account.email(), account.name(), account.role(), account.studentId(), account.passwordHash(), account.status());
        return account;
    }

    void deleteById(String id) { jdbc.update("DELETE FROM accounts WHERE id = ?", id); }

    private static AuthService.Account mapAccount(ResultSet row, int index) throws SQLException {
        return new AuthService.Account(row.getString("id"), row.getString("email"), row.getString("name"), row.getString("role_name"),
                row.getString("student_id"), row.getString("password_hash"), row.getString("status"));
    }
}

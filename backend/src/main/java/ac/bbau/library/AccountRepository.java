package ac.bbau.library;

import java.util.Optional;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepository {
    private final MongoTemplate mongo;
    AccountRepository(MongoTemplate mongo) { this.mongo = mongo; }
    boolean existsById(String id) { return mongo.exists(Query.query(Criteria.where("_id").is(id)), "accounts"); }
    Optional<AuthService.Account> findById(String id) {
        Document d = mongo.findById(id, Document.class, "accounts");
        if (d == null) return Optional.empty();
        return Optional.of(new AuthService.Account(d.getString("_id"), d.getString("email"), d.getString("name"), d.getString("role"), d.getString("studentId"), d.getString("passwordHash"), d.getString("status")));
    }
    AuthService.Account save(AuthService.Account a) {
        mongo.upsert(Query.query(Criteria.where("_id").is(a.id())), new Update().set("email",a.email()).set("name",a.name()).set("role",a.role()).set("studentId",a.studentId()).set("passwordHash",a.passwordHash()).set("status",a.status()), "accounts");
        return a;
    }
    void deleteById(String id) { mongo.remove(Query.query(Criteria.where("_id").is(id)), "accounts"); }
}

package ac.bbau.library;

import org.springframework.data.mongodb.repository.MongoRepository;

interface AccountRepository extends MongoRepository<AuthService.Account, String> {}

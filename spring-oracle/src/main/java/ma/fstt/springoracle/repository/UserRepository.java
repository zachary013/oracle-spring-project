// UserRepository.java
package ma.fstt.springoracle.repository;

import ma.fstt.springoracle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    List<User> findByAccountLocked(boolean locked);

    @Query(value = "SELECT username FROM dba_users", nativeQuery = true)
    List<String> findAllOracleUsernames();
}
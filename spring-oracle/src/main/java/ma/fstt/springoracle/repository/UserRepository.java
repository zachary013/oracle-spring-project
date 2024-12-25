package ma.fstt.springoracle.repository;

import ma.fstt.springoracle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Basic queries
    User findByUsername(String username);
    List<User> findByAccountLocked(boolean locked);

    // Oracle system queries
    @Query(value = "SELECT username FROM dba_users", nativeQuery = true)
    List<String> findAllOracleUsernames();

    @Query(value = "SELECT username FROM dba_users WHERE account_status = 'LOCKED'", nativeQuery = true)
    List<String> findLockedOracleUsers();

    @Query(value = "SELECT username FROM dba_users WHERE account_status = 'EXPIRED'", nativeQuery = true)
    List<String> findExpiredOracleUsers();

    // Application specific queries
    @Query("SELECT u FROM User u WHERE u.passwordExpiryDate < :date")
    List<User> findUsersWithExpiredPasswords(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :attempts")
    List<User> findUsersExceedingFailedAttempts(@Param("attempts") int attempts);

    // Tablespace related queries
    @Query(value = "SELECT username FROM dba_users WHERE default_tablespace = :tablespace", nativeQuery = true)
    List<String> findUsersByTablespace(@Param("tablespace") String tablespace);

    @Query(value = """
            SELECT u.username 
            FROM dba_users u 
            JOIN dba_ts_quotas q ON u.username = q.username 
            WHERE q.tablespace_name = :tablespace 
            AND q.max_bytes > :quota
            """, nativeQuery = true)
    List<String> findUsersExceedingQuota(@Param("tablespace") String tablespace, @Param("quota") long quota);

    // Role related queries
    @Query(value = """
            SELECT granted_role 
            FROM dba_role_privs 
            WHERE grantee = :username
            """, nativeQuery = true)
    List<String> findUserRoles(@Param("username") String username);

    @Query(value = """
            SELECT username 
            FROM dba_role_privs 
            WHERE granted_role = :role
            """, nativeQuery = true)
    List<String> findUsersWithRole(@Param("role") String role);

    // Statistics and monitoring
    @Query("SELECT COUNT(u) FROM User u WHERE u.accountLocked = true")
    long countLockedUsers();

    @Query(value = """
            SELECT username 
            FROM dba_users 
            WHERE last_login < SYSDATE - :days
            """, nativeQuery = true)
    List<String> findInactiveUsers(@Param("days") int days);
}
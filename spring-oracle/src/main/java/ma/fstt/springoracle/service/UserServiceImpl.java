// UserServiceImpl.java
package ma.fstt.springoracle.service;

import ma.fstt.springoracle.dto.UserDTO;
import ma.fstt.springoracle.model.User;
import ma.fstt.springoracle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public User createUser(UserDTO userDTO) {
        validatePasswordPolicy(userDTO.getPassword(), userDTO.getPasswordPolicy());

        StringBuilder sql = new StringBuilder()
                .append("CREATE USER ")
                .append(userDTO.getUsername())
                .append(" IDENTIFIED BY ")
                .append(userDTO.getPassword());

        if (userDTO.getDefaultTablespace() != null) {
            sql.append(" DEFAULT TABLESPACE ")
                    .append(userDTO.getDefaultTablespace());
        }

        if (userDTO.getTemporaryTablespace() != null) {
            sql.append(" TEMPORARY TABLESPACE ")
                    .append(userDTO.getTemporaryTablespace());
        }

        if (userDTO.getQuotaLimit() != null) {
            sql.append(" QUOTA ")
                    .append(userDTO.getQuotaLimit())
                    .append(" ON ")
                    .append(userDTO.getDefaultTablespace());
        }

        jdbcTemplate.execute(sql.toString());

        // Créer l'utilisateur dans notre base de données applicative
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setDefaultTablespace(userDTO.getDefaultTablespace());
        user.setTemporaryTablespace(userDTO.getTemporaryTablespace());
        user.setQuotaLimit(userDTO.getQuotaLimit());
        user.setPasswordExpiryDate(LocalDateTime.now().plusDays(90)); // Par défaut 90 jours
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);

        user = userRepository.save(user);

        // Appliquer les rôles si spécifiés
        if (userDTO.getRoles() != null) {
            userDTO.getRoles().forEach(role -> grantRole(userDTO.getUsername(), role));
        }

        return user;
    }

    @Override
    public User updateUser(String username, UserDTO userDTO) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé: " + username);
        }

        // Mise à jour des attributs modifiables
        if (userDTO.getDefaultTablespace() != null) {
            jdbcTemplate.execute("ALTER USER " + username +
                    " DEFAULT TABLESPACE " + userDTO.getDefaultTablespace());
            user.setDefaultTablespace(userDTO.getDefaultTablespace());
        }

        if (userDTO.getTemporaryTablespace() != null) {
            jdbcTemplate.execute("ALTER USER " + username +
                    " TEMPORARY TABLESPACE " + userDTO.getTemporaryTablespace());
            user.setTemporaryTablespace(userDTO.getTemporaryTablespace());
        }

        if (userDTO.getQuotaLimit() != null) {
            modifyQuota(username, userDTO.getDefaultTablespace(), userDTO.getQuotaLimit());
            user.setQuotaLimit(userDTO.getQuotaLimit());
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(String username) {
        jdbcTemplate.execute("DROP USER " + username + " CASCADE");

        User user = getUserByUsername(username);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void modifyQuota(String username, String tablespace, String quota) {
        jdbcTemplate.execute("ALTER USER " + username +
                " QUOTA " + quota + " ON " + tablespace);

        User user = getUserByUsername(username);
        if (user != null) {
            user.setQuotaLimit(quota);
            userRepository.save(user);
        }
    }

    @Override
    public void lockUser(String username) {
        jdbcTemplate.execute("ALTER USER " + username + " ACCOUNT LOCK");

        User user = getUserByUsername(username);
        if (user != null) {
            user.setAccountLocked(true);
            userRepository.save(user);
        }
    }

    @Override
    public void unlockUser(String username) {
        jdbcTemplate.execute("ALTER USER " + username + " ACCOUNT UNLOCK");

        User user = getUserByUsername(username);
        if (user != null) {
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    @Override
    public void resetPassword(String username, String newPassword) {
        jdbcTemplate.execute("ALTER USER " + username +
                " IDENTIFIED BY " + newPassword);

        User user = getUserByUsername(username);
        if (user != null) {
            user.setPasswordExpiryDate(LocalDateTime.now().plusDays(90));
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    @Override
    public void grantRole(String username, String role) {
        jdbcTemplate.execute("GRANT " + role + " TO " + username);
    }

    @Override
    public void revokeRole(String username, String role) {
        jdbcTemplate.execute("REVOKE " + role + " FROM " + username);
    }

    @Override
    public List<String> getUserRoles(String username) {
        return jdbcTemplate.queryForList(
                "SELECT GRANTED_ROLE FROM DBA_ROLE_PRIVS WHERE GRANTEE = ?",
                String.class,
                username.toUpperCase()
        );
    }

    @Override
    public boolean isUserLocked(String username) {
        User user = getUserByUsername(username);
        return user != null && user.isAccountLocked();
    }

    @Override
    public int getFailedLoginAttempts(String username) {
        User user = getUserByUsername(username);
        return user != null ? user.getFailedLoginAttempts() : 0;
    }

    @Override
    public void updateFailedLoginAttempts(String username) {
        User user = getUserByUsername(username);
        if (user != null) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            // Verrouiller le compte après 3 tentatives échouées
            if (user.getFailedLoginAttempts() >= 3) {
                lockUser(username);
            }
            userRepository.save(user);
        }
    }

    private void validatePasswordPolicy(String password, UserDTO.PasswordPolicy policy) {
        if (policy == null) return;

        if (password.length() < policy.getMinLength()) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins " + policy.getMinLength() + " caractères"
            );
        }

        if (policy.isRequireSpecialChar() && !password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins un caractère spécial"
            );
        }

        if (policy.isRequireNumber() && !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins un chiffre"
            );
        }

        if (policy.isRequireUpperCase() && !password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins une majuscule"
            );
        }
    }
}
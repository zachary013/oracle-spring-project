// UserService.java
package ma.fstt.springoracle.service;

import ma.fstt.springoracle.dto.UserDTO;
import ma.fstt.springoracle.model.User;
import java.util.List;

public interface UserService {
    // Opérations CRUD de base
    User createUser(UserDTO userDTO);
    User updateUser(String username, UserDTO userDTO);
    void deleteUser(String username);
    User getUserByUsername(String username);
    List<User> getAllUsers();

    // Opérations de gestion des quotas
    void modifyQuota(String username, String tablespace, String quota);

    // Opérations de gestion des comptes
    void lockUser(String username);
    void unlockUser(String username);
    void resetPassword(String username, String newPassword);

    // Opérations de gestion des rôles
    void grantRole(String username, String role);
    void revokeRole(String username, String role);
    List<String> getUserRoles(String username);

    // Opérations de surveillance
    boolean isUserLocked(String username);
    int getFailedLoginAttempts(String username);
    void updateFailedLoginAttempts(String username);
}
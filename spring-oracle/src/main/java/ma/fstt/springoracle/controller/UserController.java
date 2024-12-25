package ma.fstt.springoracle.controller;

import ma.fstt.springoracle.dto.UserDTO;
import ma.fstt.springoracle.model.User;
import ma.fstt.springoracle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // Créer un utilisateur
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        User createdUser = userService.createUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }

    // Obtenir tous les utilisateurs
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Obtenir un utilisateur par son nom
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    // Mettre à jour un utilisateur
    @PutMapping("/{username}")
    public ResponseEntity<User> updateUser(
            @PathVariable String username,
            @RequestBody UserDTO userDTO) {
        User updatedUser = userService.updateUser(username, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    // Supprimer un utilisateur
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.ok().build();
    }

    // Modifier le quota
    @PutMapping("/{username}/quota")
    public ResponseEntity<Void> modifyQuota(
            @PathVariable String username,
            @RequestParam String tablespace,
            @RequestParam String quota) {
        userService.modifyQuota(username, tablespace, quota);
        return ResponseEntity.ok().build();
    }

    // Verrouiller un utilisateur
    @PutMapping("/{username}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable String username) {
        userService.lockUser(username);
        return ResponseEntity.ok().build();
    }

    // Déverrouiller un utilisateur
    @PutMapping("/{username}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable String username) {
        userService.unlockUser(username);
        return ResponseEntity.ok().build();
    }

    // Réinitialiser le mot de passe
    @PutMapping("/{username}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> passwordMap) {
        userService.resetPassword(username, passwordMap.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    // Accorder un rôle
    @PostMapping("/{username}/roles")
    public ResponseEntity<Void> grantRole(
            @PathVariable String username,
            @RequestBody Map<String, String> roleMap) {
        userService.grantRole(username, roleMap.get("role"));
        return ResponseEntity.ok().build();
    }

    // Révoquer un rôle
    @DeleteMapping("/{username}/roles/{role}")
    public ResponseEntity<Void> revokeRole(
            @PathVariable String username,
            @PathVariable String role) {
        userService.revokeRole(username, role);
        return ResponseEntity.ok().build();
    }

    // Obtenir les rôles d'un utilisateur
    @GetMapping("/{username}/roles")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserRoles(username));
    }
}
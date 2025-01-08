package ma.fstt.springoracle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.PasswordResetRequest;
import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.dto.UserDTO;
import ma.fstt.springoracle.model.OracleUser;
import ma.fstt.springoracle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {
    @Autowired
    private final UserService userService;

    @PostMapping
    public ResponseEntity<OracleUser> createUser(@Valid @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.createUser(userDTO));
    }

    @PutMapping("/{username}")
    public ResponseEntity<OracleUser> updateUser(
            @PathVariable String username,
            @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(username, userDTO));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<OracleUser> getUser(@PathVariable String username) {
        return userService.getUser(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OracleUser>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/{username}/lock")
    public ResponseEntity<Void> lockAccount(@PathVariable String username) {
        userService.lockAccount(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/unlock")
    public ResponseEntity<Void> unlockAccount(@PathVariable String username) {
        userService.unlockAccount(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String username,
            @RequestBody PasswordResetRequest request) {
        userService.resetPassword(username, request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/roles")
    public ResponseEntity<Void> grantRole(
            @PathVariable String username,
            @Valid @RequestBody RoleDTO roleDTO) {
        userService.grantRole(username, roleDTO.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{username}/roles/bulk")
    public ResponseEntity<Void> grantMultipleRoles(
            @PathVariable String username,
            @Valid @RequestBody List<RoleDTO> roles) {
        userService.grantMultipleRoles(username, roles);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{username}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(
            @PathVariable String username,
            @PathVariable String roleName) {
        userService.revokeRole(username, roleName);
        return ResponseEntity.ok().build();
    }
}
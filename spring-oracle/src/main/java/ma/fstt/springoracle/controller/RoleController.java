package ma.fstt.springoracle.controller;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.exception.DatabaseOperationException;
import ma.fstt.springoracle.exception.PrivilegeNotFoundException;
import ma.fstt.springoracle.exception.RoleManagementException;
import ma.fstt.springoracle.exception.RoleNotFoundException;
import ma.fstt.springoracle.model.Role;
import ma.fstt.springoracle.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody RoleDTO roleDTO) {
        try {
            return ResponseEntity.ok(roleService.createRole(roleDTO));
        } catch (DatabaseOperationException | RoleManagementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{roleName}")
    public ResponseEntity<Role> getRole(@PathVariable String roleName) {
        return roleService.getRole(roleName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{roleName}")
    public ResponseEntity<Void> deleteRole(@PathVariable String roleName) {
        try {
            roleService.deleteRole(roleName);
            return ResponseEntity.ok().build();
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DatabaseOperationException | RoleManagementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{roleName}/privileges/{privilegeName}")
    public ResponseEntity<Void> grantPrivilege(
            @PathVariable String roleName,
            @PathVariable String privilegeName) {
        try {
            roleService.grantPrivilege(roleName, privilegeName);
            return ResponseEntity.ok().build();
        } catch (RoleNotFoundException | PrivilegeNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DatabaseOperationException | RoleManagementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{roleName}/privileges/{privilegeName}")
    public ResponseEntity<Void> revokePrivilege(
            @PathVariable String roleName,
            @PathVariable String privilegeName) {
        try {
            roleService.revokePrivilege(roleName, privilegeName);
            return ResponseEntity.ok().build();
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DatabaseOperationException | RoleManagementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{roleName}/privileges")
    public ResponseEntity<Set<String>> getRolePrivileges(@PathVariable String roleName) {
        try {
            return ResponseEntity.ok(roleService.getRolePrivileges(roleName));
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{roleName}/privileges")
    public ResponseEntity<Void> grantPrivileges(
            @PathVariable String roleName,
            @RequestBody Set<String> privilegeNames) {
        try {
            roleService.grantPrivileges(roleName, privilegeNames);
            return ResponseEntity.ok().build();
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (DatabaseOperationException | RoleManagementException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{roleName}/has-privilege/{privilegeName}")
    public ResponseEntity<Boolean> hasPrivilege(
            @PathVariable String roleName,
            @PathVariable String privilegeName) {
        try {
            boolean hasPrivilege = roleService.hasPrivilege(roleName, privilegeName);
            return ResponseEntity.ok(hasPrivilege);
        } catch (RoleNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/available-privileges")
    public ResponseEntity<List<String>> getAvailablePrivileges() {
        return ResponseEntity.ok(roleService.getAvailablePrivileges());
    }
}


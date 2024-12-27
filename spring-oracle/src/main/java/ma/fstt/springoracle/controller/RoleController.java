package ma.fstt.springoracle.controller;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.model.Role;
import ma.fstt.springoracle.service.RoleService;
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
        return ResponseEntity.ok(roleService.createRole(roleDTO));
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
        roleService.deleteRole(roleName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roleName}/privileges/{privilegeName}")
    public ResponseEntity<Void> grantPrivilege(
            @PathVariable String roleName,
            @PathVariable String privilegeName) {
        roleService.grantPrivilege(roleName, privilegeName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{roleName}/privileges/{privilegeName}")
    public ResponseEntity<Void> revokePrivilege(
            @PathVariable String roleName,
            @PathVariable String privilegeName) {
        roleService.revokePrivilege(roleName, privilegeName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roleName}/privileges")
    public ResponseEntity<Set<String>> getRolePrivileges(@PathVariable String roleName) {
        return ResponseEntity.ok(roleService.getRolePrivileges(roleName));
    }
}
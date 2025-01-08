package ma.fstt.springoracle.controller;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.model.Privilege;
import ma.fstt.springoracle.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/privileges")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class PrivilegeController {
    @Autowired
    private final PrivilegeService privilegeService;

    @GetMapping
    public ResponseEntity<List<String>> getAllPrivileges() {
        return ResponseEntity.ok(privilegeService.getAllPrivileges());
    }

    @GetMapping("/{name}")
    public ResponseEntity<Privilege> getPrivilege(@PathVariable @NotBlank String name) {
        return privilegeService.getPrivilege(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/grant/system")
    public ResponseEntity<Void> grantSystemPrivilege(
            @RequestParam @NotBlank String privilegeName,
            @RequestParam @NotBlank String userName,
            @RequestParam(defaultValue = "false") boolean withAdminOption) {
        privilegeService.grantSystemPrivilege(privilegeName, userName, withAdminOption);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/grant/object")
    public ResponseEntity<Void> grantObjectPrivilege(
            @RequestParam @NotBlank String privilegeName,
            @RequestParam @NotBlank String objectName,
            @RequestParam @NotBlank String userName) {
        privilegeService.grantObjectPrivilege(privilegeName, objectName, userName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke/system")
    public ResponseEntity<Void> revokeSystemPrivilege(
            @RequestParam @NotBlank String privilegeName,
            @RequestParam @NotBlank String userName) {
        privilegeService.revokeSystemPrivilege(privilegeName, userName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke/object")
    public ResponseEntity<Void> revokeObjectPrivilege(
            @RequestParam @NotBlank String privilegeName,
            @RequestParam @NotBlank String objectName,
            @RequestParam @NotBlank String userName) {
        privilegeService.revokeObjectPrivilege(privilegeName, objectName, userName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/system/{userName}")
    public ResponseEntity<List<String>> getGrantedSystemPrivileges(
            @PathVariable @NotBlank String userName) {
        return ResponseEntity.ok(privilegeService.getGrantedSystemPrivileges(userName));
    }

    @GetMapping("/object/{userName}")
    public ResponseEntity<List<String>> getGrantedObjectPrivileges(
            @PathVariable @NotBlank String userName) {
        return ResponseEntity.ok(privilegeService.getGrantedObjectPrivileges(userName));
    }
}
package ma.fstt.springoracle.service;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.model.Privilege;
import ma.fstt.springoracle.model.Role;
import ma.fstt.springoracle.repository.PrivilegeRepository;
import ma.fstt.springoracle.repository.RoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Role createRole(RoleDTO roleDTO) {
        // Create Oracle role
        String createRoleSql = String.format("CREATE ROLE \"%s\"", roleDTO.getName().toUpperCase());
        jdbcTemplate.execute(createRoleSql);

        // Create application role
        Role role = new Role();
        role.setName(roleDTO.getName().toUpperCase());

        if (roleDTO.getPrivileges() != null) {
            roleDTO.getPrivileges().forEach(privilegeName ->
                    grantPrivilege(roleDTO.getName(), privilegeName));
        }

        return roleRepository.save(role);
    }

    @Override
    public Optional<Role> getRole(String name) {
        return roleRepository.findByName(name.toUpperCase());
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteRole(String name) {
        String upperName = name.toUpperCase();

        // Drop Oracle role
        String dropRoleSql = String.format("DROP ROLE \"%s\"", upperName);
        jdbcTemplate.execute(dropRoleSql);

        // Delete application role
        roleRepository.findByName(upperName)
                .ifPresent(roleRepository::delete);
    }

    @Override
    @Transactional
    public void grantPrivilege(String roleName, String privilegeName) {
        try {
            // Grant Oracle privilege
            String grantSql = String.format("GRANT \"%s\" TO \"%s\"",
                    privilegeName.toUpperCase(),
                    roleName.toUpperCase());
            jdbcTemplate.execute(grantSql);

            // Update application role
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            Privilege privilege = privilegeRepository.findByName(privilegeName.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Privilege not found"));

            role.getPrivileges().add(privilege);
            roleRepository.save(role);
        } catch (Exception e) {
            throw new RuntimeException("Failed to grant privilege: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void revokePrivilege(String roleName, String privilegeName) {
        try {
            // Revoke Oracle privilege
            String revokeSql = String.format("REVOKE \"%s\" FROM \"%s\"",
                    privilegeName.toUpperCase(),
                    roleName.toUpperCase());
            jdbcTemplate.execute(revokeSql);

            // Update application role
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            privilegeRepository.findByName(privilegeName.toUpperCase())
                    .ifPresent(privilege -> {
                        role.getPrivileges().remove(privilege);
                        roleRepository.save(role);
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to revoke privilege: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<String> getRolePrivileges(String roleName) {
        return roleRepository.findByName(roleName.toUpperCase())
                .map(role -> role.getPrivileges().stream()
                        .map(Privilege::getName)
                        .collect(Collectors.toSet()))
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }
}
// src/main/java/ma/fstt/springoracle/service/RoleServiceImpl.java
package ma.fstt.springoracle.service;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.model.Role;
import ma.fstt.springoracle.repository.RoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Role createRole(String roleName) {
        if (existsByName(roleName)) {
            throw new RuntimeException("Role already exists: " + roleName);
        }

        // Create role in Oracle
        try {
            String sql = String.format("CREATE ROLE \"%s\"", roleName.toUpperCase());
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Oracle role: " + e.getMessage());
        }

        // Create role in application database
        Role role = new Role();
        role.setName(roleName.toUpperCase());
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(String roleName) {
        if (!existsByName(roleName)) {
            throw new RuntimeException("Role does not exist: " + roleName);
        }

        // Drop role from Oracle
        try {
            String sql = String.format("DROP ROLE \"%s\"", roleName.toUpperCase());
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete Oracle role: " + e.getMessage());
        }

        // Delete role from application database
        roleRepository.findByName(roleName.toUpperCase())
                .ifPresent(roleRepository::delete);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public boolean existsByName(String roleName) {
        return roleRepository.existsByName(roleName.toUpperCase());
    }
}

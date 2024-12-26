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
        String upperRoleName = roleName.toUpperCase();
        if (roleRepository.findByName(upperRoleName).isPresent()) {
            throw new RuntimeException("Role already exists: " + upperRoleName);
        }

        // Create Oracle role
        jdbcTemplate.execute(String.format("CREATE ROLE \"%s\"", upperRoleName));

        // Create JPA entity
        Role role = new Role();
        role.setName(upperRoleName);
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(String roleName) {
        String upperRoleName = roleName.toUpperCase();
        jdbcTemplate.execute(String.format("DROP ROLE \"%s\"", upperRoleName));
        roleRepository.findByName(upperRoleName)
                .ifPresent(role -> roleRepository.delete(role));
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public boolean existsByName(String roleName) {
        return roleRepository.findByName(roleName.toUpperCase()).isPresent();
    }
}

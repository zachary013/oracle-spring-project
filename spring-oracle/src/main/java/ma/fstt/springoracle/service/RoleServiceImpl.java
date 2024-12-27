package ma.fstt.springoracle.service;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.exception.DatabaseOperationException;
import ma.fstt.springoracle.exception.PrivilegeNotFoundException;
import ma.fstt.springoracle.exception.RoleManagementException;
import ma.fstt.springoracle.exception.RoleNotFoundException;
import ma.fstt.springoracle.model.Privilege;
import ma.fstt.springoracle.model.Role;
import ma.fstt.springoracle.repository.PrivilegeRepository;
import ma.fstt.springoracle.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Role createRole(RoleDTO roleDTO) {
        try {
            // Create Oracle role
            String createRoleSql = String.format("CREATE ROLE \"%s\"", roleDTO.getName().toUpperCase());
            jdbcTemplate.execute(createRoleSql);

            // Create application role
            Role role = new Role();
            role.setName(roleDTO.getName().toUpperCase());
            role.setDescription(roleDTO.getDescription());

            if (roleDTO.getPrivileges() != null) {
                roleDTO.getPrivileges().forEach(privilegeName ->
                        grantPrivilege(roleDTO.getName(), privilegeName));
            }

            return roleRepository.save(role);
        } catch (DataAccessException e) {
            logger.error("Database error while creating role: {}", roleDTO.getName(), e);
            throw new DatabaseOperationException("Failed to create role due to database error", e);
        } catch (Exception e) {
            logger.error("Unexpected error while creating role: {}", roleDTO.getName(), e);
            throw new RoleManagementException("Failed to create role due to unexpected error", e);
        }
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
        try {
            String upperName = name.toUpperCase();

            // Drop Oracle role
            String dropRoleSql = String.format("DROP ROLE \"%s\"", upperName);
            jdbcTemplate.execute(dropRoleSql);

            // Delete application role
            roleRepository.findByName(upperName)
                    .ifPresent(roleRepository::delete);
        } catch (DataAccessException e) {
            logger.error("Database error while deleting role: {}", name, e);
            throw new DatabaseOperationException("Failed to delete role due to database error", e);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting role: {}", name, e);
            throw new RoleManagementException("Failed to delete role due to unexpected error", e);
        }
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
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));

            Privilege privilege = privilegeRepository.findByName(privilegeName.toUpperCase())
                    .orElseThrow(() -> new PrivilegeNotFoundException("Privilege not found: " + privilegeName));

            role.getPrivileges().add(privilege);
            roleRepository.save(role);
        } catch (DataAccessException e) {
            logger.error("Database error while granting privilege: {} to role: {}", privilegeName, roleName, e);
            throw new DatabaseOperationException("Failed to grant privilege due to database error", e);
        } catch (RoleNotFoundException | PrivilegeNotFoundException e) {
            logger.error("Entity not found while granting privilege: {} to role: {}", privilegeName, roleName, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while granting privilege: {} to role: {}", privilegeName, roleName, e);
            throw new RoleManagementException("Failed to grant privilege due to unexpected error", e);
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
                    .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));

            privilegeRepository.findByName(privilegeName.toUpperCase())
                    .ifPresentOrElse(
                            privilege -> {
                                role.getPrivileges().remove(privilege);
                                roleRepository.save(role);
                            },
                            () -> logger.warn("Privilege not found in application database: {}", privilegeName)
                    );
        } catch (DataAccessException e) {
            logger.error("Database error while revoking privilege: {} from role: {}", privilegeName, roleName, e);
            throw new DatabaseOperationException("Failed to revoke privilege due to database error", e);
        } catch (RoleNotFoundException e) {
            logger.error("Role not found while revoking privilege: {} from role: {}", privilegeName, roleName, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while revoking privilege: {} from role: {}", privilegeName, roleName, e);
            throw new RoleManagementException("Failed to revoke privilege due to unexpected error", e);
        }
    }

    @Override
    public Set<String> getRolePrivileges(String roleName) {
        return roleRepository.findByName(roleName.toUpperCase())
                .map(role -> role.getPrivileges().stream()
                        .map(Privilege::getName)
                        .collect(Collectors.toSet()))
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
    }

    @Override
    @Transactional
    public void grantPrivileges(String roleName, Set<String> privilegeNames) {
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));

        for (String privilegeName : privilegeNames) {
            try {
                grantPrivilege(roleName, privilegeName);
                logger.info("Granted privilege: {} to role: {}", privilegeName, roleName);
            } catch (PrivilegeNotFoundException e) {
                logger.warn("Skipping non-existent privilege: {}", privilegeName);
            } catch (Exception e) {
                logger.error("Failed to grant privilege: {} to role: {}", privilegeName, roleName, e);
                // Decide whether to continue or throw an exception based on your requirements
            }
        }
    }

    @Override
    public boolean hasPrivilege(String roleName, String privilegeName) {
        return roleRepository.findByName(roleName.toUpperCase())
                .map(role -> role.getPrivileges().stream()
                        .anyMatch(privilege -> privilege.getName().equalsIgnoreCase(privilegeName)))
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
    }

    @Override
    public List<String> getAvailablePrivileges() {
        return privilegeRepository.findAll().stream()
                .map(Privilege::getName)
                .collect(Collectors.toList());
    }
}


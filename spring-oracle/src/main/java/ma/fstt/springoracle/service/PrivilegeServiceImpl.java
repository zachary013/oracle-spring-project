package ma.fstt.springoracle.service;

import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.dto.PrivilegeDTO;
import ma.fstt.springoracle.exception.PrivilegeOperationException;
import ma.fstt.springoracle.model.Privilege;
import ma.fstt.springoracle.repository.PrivilegeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Privilege createPrivilege(PrivilegeDTO privilegeDTO) {
        String privilegeName = privilegeDTO.getName().toUpperCase();

        if (privilegeRepository.existsByName(privilegeName)) {
            throw new PrivilegeOperationException("Privilege already exists: " + privilegeName);
        }

        if (!validatePrivilegeExists(privilegeName)) {
            throw new PrivilegeOperationException("Invalid Oracle privilege: " + privilegeName);
        }

        Privilege privilege = new Privilege();
        privilege.setName(privilegeName);
        privilege.setDescription(privilegeDTO.getDescription());

        return privilegeRepository.save(privilege);
    }

    @Override
    public Optional<Privilege> getPrivilege(String name) {
        return privilegeRepository.findByName(name.toUpperCase());
    }

    @Override
    public List<Privilege> getAllPrivileges() {
        return privilegeRepository.findAll();
    }

    @Override
    @Transactional
    public void deletePrivilege(String name) {
        privilegeRepository.findByName(name.toUpperCase())
                .ifPresent(privilegeRepository::delete);
    }

    @Override
    @Transactional
    public void grantSystemPrivilege(String privilegeName, String userName, boolean withAdminOption) {
        String sql = String.format(
                "GRANT %s TO %s%s",
                sanitizeIdentifier(privilegeName),
                sanitizeIdentifier(userName),
                withAdminOption ? " WITH ADMIN OPTION" : ""
        );
        executePrivilegeCommand(sql, "grant system privilege");
    }

    @Override
    @Transactional
    public void grantObjectPrivilege(String privilegeName, String objectName, String userName) {
        String sql = String.format(
                "GRANT %s ON %s TO %s",
                sanitizeIdentifier(privilegeName),
                sanitizeIdentifier(objectName),
                sanitizeIdentifier(userName)
        );
        executePrivilegeCommand(sql, "grant object privilege");
    }

    @Override
    @Transactional
    public void revokeSystemPrivilege(String privilegeName, String userName) {
        String sql = String.format(
                "REVOKE %s FROM %s",
                sanitizeIdentifier(privilegeName),
                sanitizeIdentifier(userName)
        );
        executePrivilegeCommand(sql, "revoke system privilege");
    }

    @Override
    @Transactional
    public void revokeObjectPrivilege(String privilegeName, String objectName, String userName) {
        String sql = String.format(
                "REVOKE %s ON %s FROM %s",
                sanitizeIdentifier(privilegeName),
                sanitizeIdentifier(objectName),
                sanitizeIdentifier(userName)
        );
        executePrivilegeCommand(sql, "revoke object privilege");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getGrantedSystemPrivileges(String userName) {
        String sql = """
            SELECT privilege 
            FROM dba_sys_privs 
            WHERE grantee = ?
            ORDER BY privilege
        """;

        return jdbcTemplate.queryForList(
                sql,
                String.class,
                userName.toUpperCase()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getGrantedObjectPrivileges(String userName) {
        String sql = """
            SELECT privilege || ' ON ' || owner || '.' || table_name as privilege_desc 
            FROM dba_tab_privs 
            WHERE grantee = ?
            ORDER BY owner, table_name, privilege
        """;

        return jdbcTemplate.queryForList(
                sql,
                String.class,
                userName.toUpperCase()
        );
    }

    private boolean validatePrivilegeExists(String privilegeName) {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT privilege FROM dba_sys_privs WHERE privilege = ?
                UNION
                SELECT privilege FROM dba_tab_privs WHERE privilege = ?
            )
        """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                privilegeName.toUpperCase(),
                privilegeName.toUpperCase()
        );

        return count != null && count > 0;
    }

    private void executePrivilegeCommand(String sql, String operation) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new PrivilegeOperationException(
                    String.format("Failed to %s: %s", operation, e.getMessage()),
                    e
            );
        }
    }

    private String sanitizeIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "").toUpperCase() + "\"";
    }
}
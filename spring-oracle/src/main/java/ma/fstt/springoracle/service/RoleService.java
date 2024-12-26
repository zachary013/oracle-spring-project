package ma.fstt.springoracle.service;

import ma.fstt.springoracle.model.Role;
import java.util.List;

public interface RoleService {
    Role createRole(String roleName);
    void deleteRole(String roleName);
    List<Role> getAllRoles();
    boolean existsByName(String roleName);
}

package ma.fstt.springoracle.service;

import ma.fstt.springoracle.dto.RoleDTO;
import ma.fstt.springoracle.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleService {
    //Role CRUDs
    Role createRole(RoleDTO roleDTO);
    Optional<Role> getRole(String name);
    List<Role> getAllRoles();
    void deleteRole(String name);

    //Grant Revoke privs
    void grantPrivilege(String roleName, String privilegeName);
    void grantPrivileges(String roleName, Set<String> privilegeNames);
    void revokePrivilege(String roleName, String privilegeName);
    Set<String> getRolePrivileges(String roleName);

    boolean hasPrivilege(String roleName, String privilegeName);
}


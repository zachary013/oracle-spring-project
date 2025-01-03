package ma.fstt.springoracle.service;

import ma.fstt.springoracle.model.Privilege;
import java.util.List;
import java.util.Optional;

public interface PrivilegeService {
    Optional<Privilege> getPrivilege(String name);

    List<String> getAllPrivileges();

    void grantSystemPrivilege(String privilegeName, String userName, boolean withAdminOption);

    void grantObjectPrivilege(String privilegeName, String objectName, String userName);

    void revokeSystemPrivilege(String privilegeName, String userName);

    void revokeObjectPrivilege(String privilegeName, String objectName, String userName);

    List<String> getGrantedSystemPrivileges(String userName);

    List<String> getGrantedObjectPrivileges(String userName);
}
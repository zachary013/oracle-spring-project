package ma.fstt.springoracle.service;

import java.util.List;

public interface RmanService {

    public List<String> listBackups();

    public void scheduledBackup();

    public void restoreDatabase(String backupFile);

    public void performIncrementalBackup();

    public void performFullBackup();

}

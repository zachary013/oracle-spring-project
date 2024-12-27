package ma.fstt.springoracle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class RmanServiceImpl implements RmanService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${rman.backup.directory}")
    private String backupDirectory;

    public void performFullBackup() {
        String backupCmd = String.format(
                "BEGIN " +
                        "  DBMS_BACKUP_RESTORE.BACKUP(" +
                        "    type => 'FULL'," +
                        "    format => '%s/full_backup_%s'," +
                        "    tag => 'FULL_BACKUP'" +
                        "  );" +
                        "END;",
                backupDirectory,
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
        );
        jdbcTemplate.execute(backupCmd);
    }



    public void performIncrementalBackup() {
        String backupCmd = String.format(
                "BEGIN " +
                        "  DBMS_BACKUP_RESTORE.BACKUP(" +
                        "    type => 'INCREMENTAL'," +
                        "    level => 1," +
                        "    format => '%s/incr_backup_%s'" +
                        "  );" +
                        "END;",
                backupDirectory,
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
        );
        jdbcTemplate.execute(backupCmd);
    }

    public void restoreDatabase(String backupFile) {
        String restoreCmd = String.format(
                "BEGIN " +
                        "  DBMS_BACKUP_RESTORE.RESTOREDB(" +
                        "    filename => '%s'" +
                        "  );" +
                        "END;",
                backupFile
        );
        jdbcTemplate.execute(restoreCmd);
    }

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void scheduledBackup() {
        try {
            log.info("Starting scheduled backup");
            performFullBackup();
            log.info("Completed scheduled backup");
        } catch (Exception e) {
            log.error("Backup failed", e);
        }
    }

    public List<String> listBackups() {
        return jdbcTemplate.queryForList(
                "SELECT backup_name FROM v$backup_files",
                String.class
        );
    }
}

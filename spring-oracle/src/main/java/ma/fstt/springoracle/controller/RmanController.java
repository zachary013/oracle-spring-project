package ma.fstt.springoracle.controller;

import ma.fstt.springoracle.service.RmanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rman")
public class RmanController {
    @Autowired
    private RmanService rmanService;

    @PostMapping("/backup/full")
    public ResponseEntity<?> fullBackup() {
        rmanService.performFullBackup();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/backup/incremental")
    public ResponseEntity<?> incrementalBackup() {
        rmanService.performIncrementalBackup();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/backups")
    public List<String> listBackups() {
        return rmanService.listBackups();
    }
}

package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationValidationTest {
    @Test void validatesOnlyEnabledProvidersAndExistingScheduleFormat() {
        Map<String, Object> config = new HashMap<>();
        ConfigurationDefaults.apply(config::put);
        ConfigurationValidation validator = new ConfigurationValidation();
        assertTrue(validator.validate(config::get).isEmpty());
        config.put("AutomaticBackups", true);
        config.put("BackupTimer.Times", List.of("25-10"));
        config.put("Sftp.UploadBackup", true);
        config.put("Sftp.Server.Port", 0);
        List<String> errors = validator.validate(config::get);
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(s -> s.contains("Fingerprint")));
    }
}

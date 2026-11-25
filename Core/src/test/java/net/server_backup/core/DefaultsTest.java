package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefaultsTest {
    @Test void existingSettingsAndMessagesRemainAvailable() {
        Map<String, Object> config = new HashMap<>();
        ConfigurationDefaults.apply(config::put);
        assertEquals(true, config.get("AutomaticBackups"));
        assertEquals(List.of("world", "world_nether", "world_the_end"), config.get("BackupWorlds"));
        assertEquals("Backups//", config.get("BackupDestination"));
        assertEquals(22, config.get("Sftp.Server.Port"));
        Map<String, Object> messages = new HashMap<>();
        MessageDefaults.apply(messages::put);
        assertEquals("Backup [%file%] saved.", messages.get("Info.BackupFinished"));
        assertNotNull(messages.get("Error.SmbUploadFailed"));
    }
}

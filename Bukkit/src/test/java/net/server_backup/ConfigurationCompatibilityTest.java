package net.server_backup;

import net.server_backup.core.ConfigurationDefaults;
import net.server_backup.core.MessageDefaults;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationCompatibilityTest {
    @Test void existingValuesOverrideDefaultsAndMissingSettingsRemainAvailable() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("BackupDestination: saved-backups\nAutomaticBackups: false\nSftp:\n  Server:\n    Port: 2222\n");
        ConfigurationDefaults.apply(config::addDefault);
        assertEquals("saved-backups", config.getString("BackupDestination"));
        assertFalse(config.getBoolean("AutomaticBackups"));
        assertEquals(2222, config.getInt("Sftp.Server.Port"));
        assertTrue(config.contains("BackupTimer.Days"));
        java.util.Map<String, Object> snapshot = new java.util.HashMap<>(config.getValues(true));
        ConfigurationDefaults.apply((key, ignored) -> snapshot.put(key, config.get(key)));
        net.server_backup.core.Settings settings = new net.server_backup.core.Settings(snapshot);
        assertEquals(2222, settings.getInt("Sftp.Server.Port"));
        assertEquals(java.util.List.of("world", "world_nether", "world_the_end"), settings.getStringList("BackupWorlds"));
        YamlConfiguration messages = new YamlConfiguration();
        messages.loadFromString("Info:\n  BackupStarted: 'Custom %file%'\n");
        MessageDefaults.apply(messages::addDefault);
        assertEquals("Custom %file%", messages.getString("Info.BackupStarted"));
        assertNotNull(messages.getString("Error.NoBackupFound"));
    }
}

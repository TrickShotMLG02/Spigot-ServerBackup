package net.server_backup.distribution;

import org.junit.jupiter.api.Test;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class DistributionIT {
    @Test void jarContainsAdaptersAndMetadataWithoutServerApisOrNewerBytecode() throws Exception {
        try (ZipFile jar = new ZipFile(System.getProperty("distribution.jar"))) {
            for (String name : new String[]{"net/server_backup/bootstrap/ServerBackupBootstrap.class",
                    "net/server_backup/platform/bukkit/BukkitPlatform.class", "net/server_backup/platform/paper/PaperPlatform.class",
                    "net/server_backup/core/BackupService.class", "net/server_backup/core/storage/SftpStorage.class"})
                assertNotNull(jar.getEntry(name), name);
            assertNull(jar.getEntry("org/bukkit/Bukkit.class"));
            assertNull(jar.getEntry("io/papermc/paper/PaperConfig.class"));
            String metadata = new String(jar.getInputStream(jar.getEntry("plugin.yml")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(metadata.contains("main: net.server_backup.bootstrap.ServerBackupBootstrap"));
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                assertFalse(entry.getName().startsWith("org/bukkit/") || entry.getName().startsWith("org/spigotmc/")
                        || entry.getName().startsWith("io/papermc/paper/") || entry.getName().startsWith("net/minecraft/"), entry.getName());
                if (entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/versions/")) {
                    try (DataInputStream input = new DataInputStream(jar.getInputStream(entry))) {
                        assertEquals(0xCAFEBABE, input.readInt(), entry.getName());
                        input.readUnsignedShort();
                        assertTrue(input.readUnsignedShort() <= 61, entry.getName());
                    }
                }
            }
        }
    }
}

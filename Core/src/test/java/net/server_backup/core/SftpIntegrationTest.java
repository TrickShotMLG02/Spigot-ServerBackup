package net.server_backup.core;

import net.server_backup.core.storage.SftpStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "SERVERBACKUP_SFTP_FINGERPRINT", matches = "SHA256:.+")
class SftpIntegrationTest {
    @TempDir Path directory;
    @Test void validatesHostAndTransfersFiles() throws Exception {
        SftpStorage rejected = new SftpStorage("127.0.0.1", 21218, "testuser", "testpass", "SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "/");
        assertThrows(IOException.class, rejected::list);
        SftpStorage storage = new SftpStorage("127.0.0.1", 21218, "testuser", "testpass", System.getenv("SERVERBACKUP_SFTP_FINGERPRINT"), "/");
        Path source = Files.writeString(directory.resolve("backup-fixture.zip"), "backup contents");
        assertTrue(storage.store(source));
        assertTrue(storage.listFiles().stream().anyMatch(f -> f.name.equals("backup-fixture.zip")));
        Path destination = directory.resolve("download.zip");
        assertTrue(storage.fetch("backup-fixture.zip", destination));
        assertEquals("backup contents", Files.readString(destination));
        storage.delete("backup-fixture.zip");
        assertTrue(storage.listFiles().stream().noneMatch(f -> f.name.equals("backup-fixture.zip")));
        assertFalse(storage.fetch("backup-fixture.zip", destination));
        assertEquals("backup contents", Files.readString(destination));
    }
}

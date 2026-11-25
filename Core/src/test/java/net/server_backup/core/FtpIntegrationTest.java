package net.server_backup.core;

import net.server_backup.core.storage.FtpStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Optional integration check against an isolated local FTP fixture. */
@EnabledIfEnvironmentVariable(named = "SERVERBACKUP_FTP_TEST_PORT", matches = "[0-9]+")
class FtpIntegrationTest {
    @TempDir Path directory;
    @Test void uploadsListsDownloadsAndDeletes() throws Exception {
        int port = Integer.parseInt(System.getenv("SERVERBACKUP_FTP_TEST_PORT"));
        FtpStorage storage = new FtpStorage("127.0.0.1", port, "testuser", "testpass", "/");
        Path source = Files.writeString(directory.resolve("backup-fixture.zip"), "backup contents");
        assertTrue(storage.store(source));
        assertTrue(storage.listFiles().stream().anyMatch(f -> f.name.equals(source.getFileName().toString())));
        Path downloaded = directory.resolve("download.zip");
        assertTrue(storage.fetch(source.getFileName().toString(), downloaded));
        assertEquals("backup contents", Files.readString(downloaded));
        assertTrue(storage.delete(source.getFileName().toString()));
        assertFalse(storage.listFiles().stream().anyMatch(f -> f.name.equals(source.getFileName().toString())));
        assertFalse(storage.fetch(source.getFileName().toString(), downloaded));
        assertEquals("backup contents", Files.readString(downloaded));
    }
}

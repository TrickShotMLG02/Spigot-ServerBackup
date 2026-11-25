package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {
    @TempDir Path directory;
    private static class FakePort implements BackupService.Port {
        Path source, target;
        String message;
        boolean full;
        public void executeAsync(Runnable task) { task.run(); }
        public void prepareBackup(String source) { }
        public void backupStarted(String name) { }
        public void archive(Path source, Path target, boolean full) { this.source = source; this.target = target; this.full = full; }
        public void message(String code, String name) { message = code; }
        public void failure(String operation, Exception exception) { fail(exception); }
    }
    private BackupService service(FakePort port) {
        return new BackupService(directory, Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC), port);
    }
    @Test void orchestratesArchiveWithLegacyName() throws IOException {
        Path source = Files.createDirectory(directory.resolve("world"));
        FakePort port = new FakePort();
        service(port).create(source.toString(), true);
        assertEquals(source, port.source);
        assertTrue(port.target.getFileName().toString().startsWith("backup-2026-01-02~03-04-05-"));
        assertTrue(port.full);
    }
    @Test void deletesNestedBackupAndRejectsTraversal() throws IOException {
        Path backup = Files.createDirectories(directory.resolve("backup/nested"));
        Files.writeString(backup.resolve("file"), "data");
        FakePort port = new FakePort();
        service(port).remove("backup");
        assertFalse(Files.exists(directory.resolve("backup")));
        assertEquals("Info.BackupRemoved", port.message);
        service(port).remove("../outside");
        assertEquals("Error.DeletionFailed", port.message);
    }
}

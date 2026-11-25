package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileBackupTest {
    @TempDir Path directory;
    @Test void keepsExistingCopiesWhenNamesCollide() throws Exception {
        Path source = Files.writeString(directory.resolve("config.yml"), "first");
        Path destination = directory.resolve("backups");
        Clock clock = Clock.fixed(Instant.ofEpochSecond(42), ZoneOffset.UTC);
        LocalFileBackup storage = new LocalFileBackup();
        Path first = storage.copy(source, destination, clock);
        Files.writeString(source, "second");
        Path second = storage.copy(source, destination, clock);
        Path third = storage.copy(source, destination, clock);
        assertEquals("first", Files.readString(first));
        assertEquals("config 42.yml", second.getFileName().toString());
        assertNotEquals(second, third);
        assertEquals("second", Files.readString(third));
    }
}

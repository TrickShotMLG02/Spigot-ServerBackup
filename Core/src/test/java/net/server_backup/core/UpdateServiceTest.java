package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceTest {
    @TempDir Path directory;
    @Test void comparesVersionComponents() {
        UpdateService updates = new UpdateService();
        assertTrue(updates.isNewer("2.12.0", "2.9.9"));
        assertFalse(updates.isNewer("2.1", "2.1.0"));
        assertFalse(updates.isNewer("2.9", "2.10"));
    }
    @Test void failedDownloadDoesNotOverwriteStagedFile() throws IOException {
        Path destination = Files.writeString(directory.resolve("update.jar"), "old");
        UpdateService updates = new UpdateService();
        assertThrows(IOException.class, () -> updates.download(directory.resolve("missing").toUri().toURL(), destination));
        assertEquals("old", Files.readString(destination));
        Path source = Files.writeString(directory.resolve("source"), "new");
        updates.download(source.toUri().toURL(), destination);
        assertEquals("new", Files.readString(destination));
    }
}

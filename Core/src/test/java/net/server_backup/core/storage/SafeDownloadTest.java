package net.server_backup.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SafeDownloadTest {
    @TempDir Path directory;
    @Test void preservesExistingFileWhenTransferFailsOrReportsMissing() throws Exception {
        Path target = Files.writeString(directory.resolve("backup.zip"), "original");
        assertFalse(SafeDownload.run(target, temp -> { Files.writeString(temp, "partial"); return false; }));
        assertEquals("original", Files.readString(target));
        assertThrows(IOException.class, () -> SafeDownload.run(target, temp -> { Files.writeString(temp, "partial"); throw new IOException("failure"); }));
        assertEquals("original", Files.readString(target));
        assertTrue(SafeDownload.run(target, temp -> { Files.writeString(temp, "complete"); return true; }));
        assertEquals("complete", Files.readString(target));
    }
}

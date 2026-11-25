package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BackupCatalogTest {
    @TempDir Path directory;
    @Test void listsAndSearchesCorrectPagesWithoutTheFilesDirectory() throws IOException {
        Files.createDirectory(directory.resolve("Files"));
        for (int i = 0; i < 12; i++) Files.writeString(directory.resolve(String.format("world-%02d.zip", i)), "data");
        BackupCatalog catalog = new BackupCatalog();
        BackupCatalog.Page second = catalog.list(directory, null, 2);
        assertEquals(12, second.total);
        assertEquals(2, second.pages());
        assertEquals("world-10.zip", second.entries.get(0).name);
        assertEquals(4, second.entries.get(0).bytes);
        assertEquals(2, catalog.list(directory, "world-1", 1).total);
        assertThrows(IllegalArgumentException.class, () -> catalog.list(directory, null, 3));
    }
}

package net.server_backup.core;

import net.server_backup.core.storage.SmbStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class SmbStorageTest {
    @TempDir Path directory;
    @Test void rejectsInvalidEndpointAndTraversalBeforeConnecting() {
        assertThrows(IllegalArgumentException.class, () -> new SmbStorage("host", 445, "share", "../unsafe", "", "", ""));
        SmbStorage storage = new SmbStorage("host", 445, "share", "", "", "", "");
        assertThrows(IllegalArgumentException.class, () -> storage.download("../secret", directory.resolve("out")));
        assertThrows(IOException.class, () -> storage.upload(directory.resolve("missing")));
    }
}

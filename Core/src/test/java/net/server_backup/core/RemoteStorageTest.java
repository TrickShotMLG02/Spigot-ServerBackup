package net.server_backup.core;

import net.server_backup.core.storage.FtpStorage;
import net.server_backup.core.storage.SftpStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RemoteStorageTest {
    @TempDir Path directory;
    @Test void ftpRejectsRemoteTraversalWithoutConnecting() {
        FtpStorage storage = new FtpStorage("localhost", 21, "u", "p", "/");
        assertThrows(IllegalArgumentException.class, () -> storage.download("../secret", directory.resolve("out")));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("sub/file"));
    }
    @Test void sftpRequiresFingerprintAndRejectsRemoteTraversal() {
        SftpStorage storage = new SftpStorage("localhost", 22, "u", "p", "", "/");
        assertThrows(IllegalArgumentException.class, () -> storage.download("../secret", directory.resolve("out")));
        assertThrows(IOException.class, storage::list);
    }
}

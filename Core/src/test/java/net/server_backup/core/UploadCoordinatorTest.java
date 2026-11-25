package net.server_backup.core;

import net.server_backup.core.storage.UploadCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class UploadCoordinatorTest {
    @TempDir Path directory;
    @Test void keepsLocalBackupWhenAnyUploadFailsAndStillTriesAllTargets() throws Exception {
        Path archive = Files.writeString(directory.resolve("backup.zip"), "data");
        AtomicInteger calls = new AtomicInteger();
        UploadCoordinator coordinator = new UploadCoordinator();
        boolean success = coordinator.upload(archive, List.of(
                UploadCoordinator.target(p -> { calls.incrementAndGet(); return false; }, true),
                UploadCoordinator.target(p -> { calls.incrementAndGet(); return true; }, false)));
        assertFalse(success);
        assertEquals(2, calls.get());
        assertTrue(Files.exists(archive));
        assertTrue(coordinator.upload(archive, List.of(UploadCoordinator.target(p -> true, true))));
        assertFalse(Files.exists(archive));
    }
}

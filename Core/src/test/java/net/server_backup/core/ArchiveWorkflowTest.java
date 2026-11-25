package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveWorkflowTest {
    @TempDir Path root;
    private static class Fake implements ArchiveWorkflow.Port {
        Path uploaded;
        public boolean isTrackedRegion(String key) { return key.endsWith("r.0.0.mca"); }
        public void clearTrackedRegions(String source) { }
        public void progress(Path path) { }
        public void skipped(Path path) { }
        public void upload(Path archive) { uploaded = archive; }
    }
    @Test void selectsDynamicRegionsAndExcludesBackupDestination() throws IOException {
        Path world = Files.createDirectories(root.resolve("world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Files.writeString(region.resolve("r.0.0.mca"), "included");
        Files.writeString(region.resolve("r.1.0.mca"), "excluded");
        Path backups = Files.createDirectory(world.resolve("backups"));
        Files.writeString(backups.resolve("old.zip"), "old");
        Fake port = new Fake();
        Path result = new ArchiveWorkflow(port).create(world, backups.resolve("backup-test.zip"), backups,
                List.of(), true, false, true);
        assertEquals(backups.resolve("dynamic-backup-test.zip"), result);
        assertEquals(result, port.uploaded);
        try (ZipFile zip = new ZipFile(result.toFile())) {
            assertNotNull(zip.getEntry("region/r.0.0.mca"));
            assertNull(zip.getEntry("region/r.1.0.mca"));
            assertNull(zip.getEntry("backups/old.zip"));
        }
    }
}

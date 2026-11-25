package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveCommandPlannerTest {
    @TempDir Path root;
    @Test void plansArchivesAndRejectsTraversalAndCollisions() throws Exception {
        Files.createDirectory(root.resolve("world"));
        ArchiveCommandPlanner planner = new ArchiveCommandPlanner();
        var zip = planner.plan(root, "world", false);
        assertNull(zip.error);
        assertEquals(root.resolve("world.zip"), zip.target);
        assertEquals(ArchiveCommandPlanner.Error.INVALID_PATH, planner.plan(root, "../escape", false).error);
        Files.writeString(zip.target, "archive");
        assertEquals(ArchiveCommandPlanner.Error.TARGET_EXISTS, planner.plan(root, "world", false).error);
        assertEquals(ArchiveCommandPlanner.Error.TARGET_EXISTS, planner.plan(root, "world.zip", true).error);
        assertEquals(ArchiveCommandPlanner.Error.NOT_ZIP, planner.plan(root, "world", true).error);
    }
}

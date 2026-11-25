package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BackupTimerTest {
    private static class Fake implements BackupTimer.Port {
        List<String> created = new ArrayList<>();
        boolean shutdown, running;
        int deleted;
        public boolean automaticBackups() { return true; }
        public List<String> days() { return List.of("MONDAY"); }
        public List<String> times() { return List.of("00-00"); }
        public List<String> worlds() { return List.of("world"); }
        public void createBackup(String world) { created.add(world); }
        public void invalidSchedule(String value, Exception error) { fail(error); }
        public int backupLimit() { return 1; }
        public int maxAgeDays() { return 0; }
        public boolean keepUnique() { return false; }
        public File[] backupEntries() { return new File[]{new File("backup-2025-01-01~00-00-00-world.zip"), new File("backup-2025-01-02~00-00-00-world.zip")}; }
        public void removeBackup(File entry) { deleted++; }
        public void unavailableBackupDirectory() { fail(); }
        public boolean shutdownRequested() { return true; }
        public boolean tasksRunning() { return running; }
        public void shutdown() { shutdown = true; }
    }
    @Test void schedulesOncePerMinuteAndWaitsForTasksToFinish() {
        Fake port = new Fake(); port.running = true;
        BackupTimer timer = new BackupTimer(port);
        LocalDateTime monday = LocalDateTime.of(2026, 11, 23, 0, 0);
        timer.tick(monday); timer.tick(monday.plusSeconds(30));
        assertEquals(List.of("world"), port.created);
        assertFalse(port.shutdown);
        assertEquals(2, port.deleted);
        port.running = false;
        timer.tick(monday.plusMinutes(1));
        assertTrue(port.shutdown);
    }
}

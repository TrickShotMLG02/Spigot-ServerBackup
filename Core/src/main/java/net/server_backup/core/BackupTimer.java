package net.server_backup.core;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/** Executes schedule, retention and deferred-shutdown decisions on each timer tick. */
public final class BackupTimer {
    public interface Port {
        boolean automaticBackups();
        List<String> days();
        List<String> times();
        List<String> worlds();
        void createBackup(String world);
        void invalidSchedule(String value, Exception error);
        int backupLimit();
        int maxAgeDays();
        boolean keepUnique();
        File[] backupEntries();
        void removeBackup(File entry);
        void unavailableBackupDirectory();
        boolean shutdownRequested();
        boolean tasksRunning();
        void shutdown();
    }
    private final Port port;
    private LocalDateTime lastMinute;
    public BackupTimer(Port port) { this.port = port; }

    public void tick(LocalDateTime now) {
        LocalDateTime minute = now.withSecond(0).withNano(0);
        if (port.automaticBackups() && !minute.equals(lastMinute)) {
            lastMinute = minute;
            for (String time : port.times()) {
                try {
                    if (new BackupSchedule().matches(now.getDayOfWeek(), now.toLocalTime(), port.days(), time))
                        for (String world : port.worlds()) port.createBackup(world);
                } catch (Exception e) { port.invalidSchedule(time, e); }
            }
        }
        int limit = port.backupLimit(), age = port.maxAgeDays();
        if (limit > 0 || (age > 0 && now.getHour() == 0 && now.getMinute() == 0)) {
            File[] entries = port.backupEntries();
            if (entries == null) port.unavailableBackupDirectory();
            else for (File file : new RetentionPolicy().select(entries, limit, age, port.keepUnique(), now.toLocalDate()))
                port.removeBackup(file);
        }
        if (port.shutdownRequested() && !port.tasksRunning()) port.shutdown();
    }
}

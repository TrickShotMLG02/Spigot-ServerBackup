package net.server_backup.bukkit;

import net.server_backup.core.*;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;

/** Bukkit adapter for the platform-independent timer decisions. */
public class Timer implements Runnable {
    private final ServerBackup plugin;
    private final boolean automatic;
    private final List<String> days, times, worlds;
    private final int limit, maxAge;
    private final boolean keepUnique;
    private final BackupTimer timer;
    public Timer(ServerBackup plugin) {
        this.plugin = plugin;
        var settings = plugin.configuration().settings;
        automatic = settings.getBoolean("AutomaticBackups");
        days = List.copyOf(settings.getStringList("BackupTimer.Days"));
        times = List.copyOf(settings.getStringList("BackupTimer.Times"));
        worlds = List.copyOf(settings.getStringList("BackupWorlds"));
        limit = settings.getInt("BackupLimiter");
        maxAge = settings.getInt("DeleteOldBackups");
        keepUnique = settings.getBoolean("KeepUniqueBackups");
        timer = new BackupTimer(new BackupTimer.Port() {
        public boolean automaticBackups() { return automatic; }
        public List<String> days() { return days; }
        public List<String> times() { return times; }
        public List<String> worlds() { return worlds; }
        public void createBackup(String world) {
            plugin.platform().runOnServerThread(() -> new Backup(plugin, world, Bukkit.getConsoleSender(),
                    !plugin.configuration().settings.getBoolean("DynamicBackup")).create());
        }
        public void invalidSchedule(String value, Exception error) {
            plugin.getLogger().log(Level.WARNING, "Invalid backup schedule: " + value, error);
        }
        public int backupLimit() { return limit; }
        public int maxAgeDays() { return maxAge; }
        public boolean keepUnique() { return keepUnique; }
        public File[] backupEntries() { return new File(plugin.configuration().backupDestination).listFiles(); }
        public void removeBackup(File file) {
            boolean removed = file.delete();
            plugin.getLogger().log(removed ? Level.INFO : Level.WARNING,
                    (removed ? "Backup removed: " : "Could not remove backup: ") + file.getName());
        }
        public void unavailableBackupDirectory() { plugin.getLogger().warning("Cannot read backup directory."); }
        public boolean shutdownRequested() { return plugin.operations().shutdown.isRequested(); }
        public boolean tasksRunning() { return !plugin.operations().tasks.isEmpty() || plugin.platform().hasActiveWork(); }
        public void shutdown() {
            plugin.platform().runOnServerThread(() -> {
                if (plugin.operations().shutdown.isRequested() && plugin.operations().tasks.isEmpty()
                        && !plugin.platform().hasActiveWork()) Bukkit.shutdown();
            });
        }
    });
    }
    public void run() { timer.tick(LocalDateTime.now()); }
}

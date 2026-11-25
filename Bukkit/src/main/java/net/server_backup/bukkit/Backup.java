package net.server_backup.bukkit;

import net.server_backup.core.*;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.logging.Level;

/** Bukkit bridge for the platform-independent backup service. */
public class Backup {
    private final ServerBackup plugin;
    private final String name;
    private final boolean full;
    private final BackupService service;

    public Backup(ServerBackup plugin, String name, CommandSender sender, boolean full) {
        this.plugin = plugin;
        this.name = name;
        this.full = full;
        this.service = new BackupService(Paths.get(plugin.configuration().backupDestination), Clock.systemDefaultZone(), new BackupService.Port() {
            public void executeAsync(Runnable task) {
                plugin.platform().runAsync(task);
            }
            public void prepareBackup(String source) {
                if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("World saves require the server thread");
                if (source.equalsIgnoreCase("@server")) {
                    Bukkit.savePlayers();
                    for (org.bukkit.World world : Bukkit.getWorlds()) world.save();
                } else {
                    Path requested = Paths.get(source).toAbsolutePath().normalize();
                    for (org.bukkit.World world : Bukkit.getWorlds()) {
                        if (world.getName().equalsIgnoreCase(source)
                                || world.getWorldFolder().toPath().toAbsolutePath().normalize().equals(requested)) {
                            world.save();
                        }
                    }
                }
            }
            public void backupStarted(String file) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("backup.notification"))
                        player.sendMessage(plugin.operations().processMessage("Info.BackupStarted").replace("%file%", file));
                }
            }
            public void archive(Path source, Path destination, boolean isFull) throws IOException {
                new ZipManager(plugin, source.toString(), destination.toString(), Bukkit.getConsoleSender(), true, true, isFull).zip();
            }
            public void message(String code, String file) {
                net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage(code).replace("%file%", file));
            }
            public void failure(String operation, Exception error) {
                plugin.getLogger().log(Level.WARNING, "Backup " + operation + " failed", error);
            }
        });
    }

    public String getBackupFilePath() { return name; }
    public void create() { service.create(name, full); }
    public void remove() { service.remove(name); }
}

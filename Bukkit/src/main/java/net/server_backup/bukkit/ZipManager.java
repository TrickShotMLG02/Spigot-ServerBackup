package net.server_backup.bukkit;

import net.server_backup.core.*;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.utils.DropboxManager;
import net.server_backup.utils.FtpManager;
import net.server_backup.utils.SftpManager;
import net.server_backup.utils.SmbManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipManager {

    private final ServerBackup plugin;
    private final String sourceFilePath;
    private String targetFilePath;
    private final CommandSender sender;
    private final boolean sendDebugMessage;
    private final boolean isSaving;
    private final boolean isFullBackup;
    private final String backupDestination;
    private final java.util.List<String> blacklist;
    private final boolean dynamicBackup;
    private final boolean logMessages;
    private final boolean consoleSender;

    public ZipManager(ServerBackup plugin, String sourceFilePath, String targetFilePath, CommandSender sender, boolean sendDebugMessage,
                      boolean isSaving, boolean isFullBackup) {
        this.plugin = plugin;
        this.sourceFilePath = sourceFilePath;
        this.targetFilePath = targetFilePath;
        this.sender = sender;
        this.sendDebugMessage = sendDebugMessage;
        this.isSaving = isSaving;
        this.isFullBackup = isFullBackup;
        this.backupDestination = plugin.configuration().backupDestination;
        this.blacklist = java.util.List.copyOf(plugin.configuration().settings.getStringList("Blacklist"));
        this.dynamicBackup = plugin.configuration().settings.getBoolean("DynamicBackup");
        this.logMessages = plugin.configuration().settings.getBoolean("SendLogMessages");
        this.consoleSender = Bukkit.getConsoleSender() == sender;
    }

    public void zip() throws IOException {
        String task = "CREATE {" + sourceFilePath.replace("\\", "/") + "}";
        plugin.operations().tasks.add(task);
        try {
        plugin.platform().runAsync(() -> {
            long sTime = System.nanoTime();

            Bukkit.getLogger().log(Level.INFO, "");
            Bukkit.getLogger().log(Level.INFO, "ServerBackup | Start zipping...");
            Bukkit.getLogger().log(Level.INFO, "");

            try {
                Path completed = workflow().create(Paths.get(sourceFilePath), Paths.get(targetFilePath),
                        Paths.get(backupDestination), blacklist, isSaving, isFullBackup, dynamicBackup);
                targetFilePath = completed.toString();
                long time = (System.nanoTime() - sTime) / 1000000;
                plugin.getLogger().log(Level.INFO, "ServerBackup | Files zipped. [" + time + "ms]");
                net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Command.Zip.Footer").replace("%file%", sourceFilePath));
                plugin.platform().runOnServerThread(() -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("backup.notification"))
                            player.sendMessage(plugin.operations().processMessage("Info.BackupFinished").replace("%file%", sourceFilePath));
                    }
                });
            } catch (IOException | RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Error while zipping files.", e);
            } finally {
                plugin.operations().tasks.remove(task);
            }
        });
        } catch (RuntimeException e) {
            plugin.operations().tasks.remove(task);
            throw e;
        }

        String afterCommand = plugin.configuration().settings.getString("CommandAfterAutomaticBackup");
        if (afterCommand != null && !afterCommand.equalsIgnoreCase("/")
                && plugin.operations().postCommandScheduled.compareAndSet(false, true)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (plugin.operations().tasks.isEmpty() && !plugin.platform().hasActiveWork()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), afterCommand.replaceAll("/", ""));
                            plugin.operations().postCommandScheduled.set(false);
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 20 * 5, 20 * 5);
        }
    }

    public void unzip() {
        String task = "UNZIP {" + sourceFilePath.replace("\\", "/") + "}";
        plugin.operations().tasks.add(task);
        try {
        plugin.platform().runAsync(() -> {

            try {

            long sTime = System.nanoTime();

            plugin.getLogger().log(Level.INFO, "");
            plugin.getLogger().log(Level.INFO, "ServerBackup | Start unzipping...");
            plugin.getLogger().log(Level.INFO, "");

            try {
                workflow().extract(Paths.get(sourceFilePath), Paths.get(targetFilePath));
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error while unzipping files.", e);
                return;
            }

            long time = (System.nanoTime() - sTime) / 1000000;

            plugin.getLogger().log(Level.INFO, "");
            plugin.getLogger().log(Level.INFO, "ServerBackup | Files unzipped. [" + time + "ms]");
            plugin.getLogger().log(Level.INFO, "");

            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Command.Unzip.Footer").replaceAll("%file%", sourceFilePath));
            } finally {
                plugin.operations().tasks.remove(task);
            }
        });
        } catch (RuntimeException e) {
            plugin.operations().tasks.remove(task);
            throw e;
        }
    }

    private ArchiveWorkflow workflow() {
        return new ArchiveWorkflow(new ArchiveWorkflow.Port() {
            public boolean isTrackedRegion(String key) {
                synchronized (plugin.configuration().backupInfo) { return plugin.configuration().backupInfo.contains(key); }
            }
            public void clearTrackedRegions(String source) {
                synchronized (plugin.configuration().backupInfo) {
                    plugin.configuration().backupInfo.set("Data." + source, "");
                    plugin.configuration().saveBackupInfo();
                }
            }
            public void progress(Path path) {
                if (sendDebugMessage && logMessages) {
                    plugin.getLogger().info("Processing '" + path + "'");
                    if (!consoleSender) net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Processing '" + path + "'");
                }
            }
            public void skipped(Path path) { net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Found '" + path + "' in blacklist. Skipping file."); }
            public void upload(Path archive) throws IOException {
                var config = plugin.configuration().settings;
                var targets = new java.util.ArrayList<net.server_backup.core.storage.UploadCoordinator.Target>();
                if (config.getBoolean("Ftp.UploadBackup")) targets.add(net.server_backup.core.storage.UploadCoordinator.target(
                        p -> new FtpManager(plugin, sender).upload(p.toString(), false), config.getBoolean("Ftp.DeleteLocalBackup")));
                if (config.getBoolean("Sftp.UploadBackup")) targets.add(net.server_backup.core.storage.UploadCoordinator.target(
                        p -> new SftpManager(plugin, sender).upload(p.toString(), false), config.getBoolean("Sftp.DeleteLocalBackup")));
                if (config.getBoolean("Smb.UploadBackup")) targets.add(net.server_backup.core.storage.UploadCoordinator.target(
                        p -> new SmbManager(plugin, sender).upload(p.toString(), false), config.getBoolean("Smb.DeleteLocalBackup")));
                if (config.getBoolean("CloudBackup.Dropbox")) targets.add(net.server_backup.core.storage.UploadCoordinator.target(
                        p -> new DropboxManager(plugin, sender).upload(p.toString(), false), config.getBoolean("CloudBackup.Options.DeleteLocalBackup")));
                if (!new net.server_backup.core.storage.UploadCoordinator().upload(archive, targets))
                    throw new IOException("One or more backup uploads failed; the local archive was kept");
            }
        });
    }

}

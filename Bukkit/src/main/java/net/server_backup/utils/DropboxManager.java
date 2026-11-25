package net.server_backup.utils;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.core.storage.DropboxStorage;
import org.bukkit.command.CommandSender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

/** Bukkit configuration and notification adapter for Dropbox storage. */
public class DropboxManager {
    private final ServerBackup plugin;
    private final CommandSender sender;
    public DropboxManager(ServerBackup plugin, CommandSender sender) { this.plugin = plugin; this.sender = sender; }
    public void uploadToDropbox(String filePath) { upload(filePath, true); }
    public boolean upload(String filePath, boolean allowDelete) {
        Path file = Paths.get(filePath);
        if (!Files.exists(file)) file = Paths.get(plugin.configuration().backupDestination).resolve(filePath);
        if (!Files.isRegularFile(file)) {
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Dropbox: Backup '" + file.getFileName() + "' not found.");
            return false;
        }
        String task = "DROPBOX UPLOAD {" + file.getFileName() + "}";
        plugin.operations().tasks.add(task);
        java.util.concurrent.atomic.AtomicReference<String> currentTask = new java.util.concurrent.atomic.AtomicReference<>(task);
        try {
            DropboxStorage storage;
            synchronized (plugin.configuration().cloudInfo) {
            storage = new DropboxStorage(plugin.configuration().cloudInfo.getString("Cloud.Dropbox.AppKey"),
                    plugin.configuration().cloudInfo.getString("Cloud.Dropbox.AppSecret"),
                    plugin.configuration().cloudInfo.getString("Cloud.Dropbox.AccessToken"),
                    plugin.configuration().cloudInfo.getString("Cloud.Dropbox.RT"), token -> {
                        plugin.configuration().cloudInfo.set("Cloud.Dropbox.RT", token);
                        plugin.configuration().saveCloud();
                    });
            }
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Dropbox: Uploading backup [" + file.getFileName() + "] ...");
            long size = Files.size(file);
            String fileName = file.getFileName().toString();
            storage.upload(file, plugin.configuration().settings.getString("CloudBackup.Options.Destination", "/"), transferred -> {
                long percent = size == 0 ? 100 : Math.min(100, Math.round(transferred * 100.0 / size));
                String next = "DROPBOX UPLOAD {" + fileName + ", Progress: " + percent + "%}";
                String previous = currentTask.getAndSet(next);
                plugin.operations().tasks.replace(previous, next);
            });
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Dropbox: Upload successfully. Backup stored on your dropbox account.");
            if (allowDelete && plugin.configuration().settings.getBoolean("CloudBackup.Options.DeleteLocalBackup")) Files.delete(file);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Dropbox upload failed", e);
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, "Dropbox: Upload failed. Check the server log for details.");
            return false;
        } finally { plugin.operations().tasks.remove(currentTask.get()); }
    }
}

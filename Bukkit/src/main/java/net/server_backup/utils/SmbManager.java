package net.server_backup.utils;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.core.storage.SmbStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/** Adapts SMB storage notifications and settings to Bukkit. */
public final class SmbManager {
    private final ServerBackup plugin;
    private final CommandSender sender;
    public SmbManager(ServerBackup plugin, CommandSender sender) { this.plugin = plugin; this.sender = sender; }
    private SmbStorage storage() {
        var c = plugin.configuration().settings;
        return new SmbStorage(c.getString("Smb.Server.Host", ""), c.getInt("Smb.Server.Port", 445),
                c.getString("Smb.Server.Share", ""), c.getString("Smb.Server.Directory", ""),
                c.getString("Smb.Server.Domain", ""), c.getString("Smb.Server.User", ""), c.getString("Smb.Server.Password", ""));
    }
    public void uploadBackup(String path) { upload(path, true); }
    public boolean upload(String path, boolean allowDelete) {
        Path source = new File(path).toPath();
        String task = "SMB UPLOAD {" + source.getFileName() + "}";
        plugin.operations().tasks.add(task);
        try {
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Info.SmbUpload").replace("%file%", source.getFileName().toString()));
            storage().store(source);
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Info.SmbUploadSuccess"));
            if (allowDelete && plugin.configuration().settings.getBoolean("Smb.DeleteLocalBackup")) {
                try { Files.delete(source); } catch (Exception e) { net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.SmbLocalDeletionFailed")); }
            }
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "SMB backup upload failed: " + e.getMessage());
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.SmbUploadFailed"));
            return false;
        } finally { plugin.operations().tasks.remove(task); }
    }
    public List<String> getSmbBackupList() {
        try { return storage().list(); }
        catch (Exception e) { Bukkit.getLogger().log(Level.WARNING, "SMB backup listing failed: " + e.getMessage()); return Collections.emptyList(); }
    }
    public void downloadBackup(String name) {
        try {
            if (storage().fetch(name, new File(plugin.configuration().backupDestination, name).toPath()))
                net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Info.SmbDownloadSuccess"));
            else net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.SmbNotFound").replace("%file%", name));
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "SMB backup download failed: " + e.getMessage());
            net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.SmbDownloadFailed"));
        }
    }
}

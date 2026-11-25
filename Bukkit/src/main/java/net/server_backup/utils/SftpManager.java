package net.server_backup.utils;

import net.server_backup.core.storage.RemoteFile;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.core.storage.SftpStorage;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;

/** Bukkit adapter for SFTP configuration and notifications. */
public class SftpManager {
    private final ServerBackup plugin;
    private final CommandSender sender;
    public SftpManager(ServerBackup plugin, CommandSender sender) { this.plugin = plugin; this.sender = sender; }
    private SftpStorage storage() {
        var c = plugin.configuration().settings;
        return new SftpStorage(c.getString("Sftp.Server.IP", "127.0.0.1"), c.getInt("Sftp.Server.Port", 22),
                c.getString("Sftp.Server.User", ""), c.getString("Sftp.Server.Password", ""),
                c.getString("Sftp.Server.Fingerprint", ""), c.getString("Sftp.Server.BackupDirectory", "Backups/"));
    }
    private void message(String code, String name) { net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage(code).replace("%file%", name)); }
    private void failure(String operation, Exception e) { plugin.getLogger().log(Level.WARNING, "SFTP " + operation + " failed", e); }
    private boolean safe(String name) { return name != null && !name.isBlank() && !name.equals(".") && !name.equals("..") && !name.contains("/") && !name.contains("\\"); }
    public void uploadFileToSftp(String path, boolean direct) { upload(path, true); }
    public boolean upload(String path, boolean allowDelete) {
        Path source = Paths.get(path);
        if (!Files.exists(source)) source = Paths.get(plugin.configuration().backupDestination).resolve(path);
        try {
            source = source.toRealPath();
            if (!source.startsWith(Paths.get(plugin.configuration().backupDestination).toRealPath()) || !Files.isRegularFile(source)) {
                message("Error.NoBackupFound", new File(path).getName()); return false;
            }
        } catch (Exception e) { message("Error.NoBackupFound", new File(path).getName()); return false; }
        String task = "SFTP UPLOAD {" + source + "}"; plugin.operations().tasks.add(task);
        try {
            SftpStorage remote = storage(); String name = source.getFileName().toString();
            message("Info.SftpUpload", name); remote.store(source);
            long size = Files.size(source);
            if (remote.listFiles().stream().noneMatch(f -> f.name.equals(name) && f.size == size)) {
                message("Error.SftpUploadFailed", name); return false;
            }
            message("Info.SftpUploadSuccess", name);
            if (allowDelete && plugin.configuration().settings.getBoolean("Sftp.DeleteLocalBackup")) {
                Files.delete(source);
            }
            return true;
        } catch (Exception e) { failure("upload", e); message("Error.SftpUploadFailed", path); return false; }
        finally { plugin.operations().tasks.remove(task); }
    }
    public void downloadFileFromSftp(String name) {
        if (!safe(name)) { message("Error.SftpNotFound", String.valueOf(name)); return; }
        Path destination = Paths.get(plugin.configuration().backupDestination).resolve(name);
        try {
            SftpStorage remote = storage();
            if (remote.listFiles().stream().noneMatch(f -> f.name.equals(name))) { message("Error.SftpNotFound", name); return; }
            message("Info.SftpDownload", name);
            if (!remote.fetch(name, destination)) { message("Error.SftpNotFound", name); return; }
            message("Info.SftpDownloadSuccess", name);
        } catch (Exception e) { failure("download", e); message("Error.SftpDownloadFailed", name); }
    }
    public void deleteFile(String name) {
        if (!safe(name)) { message("Error.SftpNotFound", String.valueOf(name)); return; }
        try { message("Info.SftpDeletion", name); storage().delete(name); message("Info.SftpDeletionSuccess", name); }
        catch (Exception e) { failure("deletion", e); message("Error.SftpDeletionFailed", name); }
    }
    public List<String> getSftpBackupList(boolean rawList) {
        List<String> result = new ArrayList<>();
        try {
            int n = 1;
            for (RemoteFile file : storage().listFiles()) {
                double size = Math.round(file.size / 1000000.0 * 100.0) / 100.0;
                result.add(rawList ? file.path + ":" + size : "\u00a77[" + n + "]\u00a7f " + file.name + " \u00a77[" + size + "MB]"); n++;
            }
        } catch (Exception e) { failure("listing", e); message("Error.SftpConnectionFailed", ""); }
        return result;
    }
}

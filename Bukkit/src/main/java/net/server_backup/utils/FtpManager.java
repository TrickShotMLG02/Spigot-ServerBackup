package net.server_backup.utils;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.core.storage.FtpStorage;
import net.server_backup.core.storage.RemoteFile;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;

/** Bukkit adapter for FTP storage. */
public class FtpManager {
    private final ServerBackup plugin;
    private final CommandSender sender;
    public FtpManager(ServerBackup plugin, CommandSender sender) { this.plugin = plugin; this.sender = sender; }
    private FtpStorage storage() {
        var c = plugin.configuration().settings;
        return new FtpStorage(c.getString("Ftp.Server.IP"), c.getInt("Ftp.Server.Port"), c.getString("Ftp.Server.User"),
                c.getString("Ftp.Server.Password"), c.getString("Ftp.Server.BackupDirectory"));
    }
    private void message(String code, String name) { net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage(code).replace("%file%", name)); }
    private void failure(String operation, Exception e) { plugin.getLogger().log(Level.WARNING, "FTP " + operation + " failed", e); }
    public void uploadFileToFtp(String path, boolean direct) { upload(path, true); }
    public boolean upload(String path, boolean allowDelete) {
        Path source = Paths.get(path);
        if (!Files.exists(source)) source = Paths.get(plugin.configuration().backupDestination).resolve(path);
        if (!Files.isRegularFile(source)) { message("Error.NoBackupFound", source.getFileName().toString()); return false; }
        String task = "FTP UPLOAD {" + source + "}"; plugin.operations().tasks.add(task);
        try {
            FtpStorage remote = storage(); message("Info.FtpUpload", source.getFileName().toString());
            if (!remote.store(source)) { message("Error.FtpUploadFailed", source.toString()); return false; }
            final String name = source.getFileName().toString();
            long size = Files.size(source);
            if (remote.listFiles().stream().noneMatch(f -> f.name.equalsIgnoreCase(name) && f.size == size)) {
                message("Error.FtpUploadFailed", name); return false;
            }
            message("Info.FtpUploadSuccess", source.toString());
            if (allowDelete && plugin.configuration().settings.getBoolean("Ftp.DeleteLocalBackup")) {
                Files.delete(source);
            }
            return true;
        } catch (Exception e) { failure("upload", e); message("Error.FtpUploadFailed", path); return false; }
        finally { plugin.operations().tasks.remove(task); }
    }
    public void downloadFileFromFtp(String path) {
        String name = new File(path).getName();
        Path destination = Paths.get(plugin.configuration().backupDestination).resolve(name);
        try {
            message("Info.FtpDownload", name);
            if (storage().fetch(name, destination)) message("Info.FtpDownloadSuccess", name);
            else message("Error.FtpDownloadFailed", name);
        } catch (Exception e) { failure("download", e); message("Error.FtpDownloadFailed", name); }
    }
    public void deleteFile(String path) {
        String name = new File(path).getName();
        try { message("Info.FtpDeletion", name); message(storage().delete(name) ? "Info.FtpDeletionSuccess" : "Error.FtpDeletionFailed", name); }
        catch (Exception e) { failure("deletion", e); message("Error.FtpDeletionFailed", name); }
    }
    public List<String> getFtpBackupList(boolean rawList) {
        List<String> result = new ArrayList<>();
        try {
            int n = 1;
            for (RemoteFile file : storage().listFiles()) {
                double size = Math.round(file.size / 1000000.0 * 100.0) / 100.0;
                result.add(rawList ? file.name + ":" + file.size : "\u00a77[" + n + "]\u00a7f " + file.name + " \u00a77[" + size + "MB]");
                n++;
            }
        } catch (Exception e) { failure("listing", e); }
        return result;
    }
}

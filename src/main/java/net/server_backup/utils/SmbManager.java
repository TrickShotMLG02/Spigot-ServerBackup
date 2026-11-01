package net.server_backup.utils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/** SMB2/3 backup uploader. Credentials are supplied through jcifs authentication, never embedded in URLs. */
public final class SmbManager {
    private final CommandSender sender;

    public SmbManager(CommandSender sender) { this.sender = sender; }

    public void uploadBackup(String path) {
        File source = new File(path);
        if (!source.isFile() || !source.canRead()) {
            sender.sendMessage(OperationHandler.processMessage("Error.SmbUploadFailed"));
            return;
        }
        String host = ServerBackup.getInstance().getConfig().getString("Smb.Server.Host", "").trim();
        String share = ServerBackup.getInstance().getConfig().getString("Smb.Server.Share", "").trim();
        String directory = ServerBackup.getInstance().getConfig().getString("Smb.Server.Directory", "").trim();
        String user = ServerBackup.getInstance().getConfig().getString("Smb.Server.User", "");
        String password = ServerBackup.getInstance().getConfig().getString("Smb.Server.Password", "");
        String domain = ServerBackup.getInstance().getConfig().getString("Smb.Server.Domain", "");
        int port = ServerBackup.getInstance().getConfig().getInt("Smb.Server.Port", 445);
        if (!host.matches("[A-Za-z0-9._:-]+") || !share.matches("[A-Za-z0-9_$.-]+") || port < 1 || port > 65535
                || directory.startsWith("/") || directory.contains("..") || directory.contains("\\")) {
            sender.sendMessage(OperationHandler.processMessage("Error.SmbUploadFailed"));
            return;
        }
        String remotePath = "smb://" + host + ":" + port + "/" + share + "/" + (directory.isEmpty() ? "" : directory.replaceAll("/+$", "") + "/");
        CIFSContext baseContext = SingletonContext.getInstance();
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(baseContext, domain, user, password);
        CIFSContext context = baseContext.withCredentials(auth);
        String task = "SMB UPLOAD {" + source.getName() + "}";
        OperationHandler.tasks.add(task);
        try {
            SmbFile folder = new SmbFile(remotePath, context);
            folder.mkdirs();
            SmbFile destination = new SmbFile(folder, source.getName());
            sender.sendMessage(OperationHandler.processMessage("Info.SmbUpload").replace("%file%", source.getName()));
            try (FileInputStream in = new FileInputStream(source); OutputStream out = new SmbFileOutputStream(destination)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            if (destination.length() != source.length()) throw new java.io.IOException("Remote size verification failed");
            sender.sendMessage(OperationHandler.processMessage("Info.SmbUploadSuccess"));
            if (ServerBackup.getInstance().getConfig().getBoolean("Smb.DeleteLocalBackup") && !source.delete()) {
                sender.sendMessage(OperationHandler.processMessage("Error.SmbLocalDeletionFailed"));
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "SMB backup upload failed: " + e.getMessage());
            sender.sendMessage(OperationHandler.processMessage("Error.SmbUploadFailed"));
        } finally {
            OperationHandler.tasks.remove(task);
        }
    }
}

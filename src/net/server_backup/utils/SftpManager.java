package net.server_backup.utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.server_backup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.List;

public class SftpManager {

    private CommandSender sender;

    private static final String server = ServerBackup.getInstance().getConfig().getString("Sftp.Server.IP");
    private static final int port = ServerBackup.getInstance().getConfig().getInt("Sftp.Server.Port");
    private static final String user = ServerBackup.getInstance().getConfig().getString("Sftp.Server.User");
    private static final String pass = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Password");

    public SftpManager(CommandSender sender) {
        this.sender = sender;
    }

    boolean isSSL = true;

    ServerBackup serverBackup = ServerBackup.getInstance();

    public void uploadFileToSftp(String filePath, boolean direct) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void downloadFileFromSftp(String filePath) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void deleteFile(String filePath) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<String> getSftpBackupList(boolean rawList) {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private SFTPClient connect() throws IOException {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void disconnect(SFTPClient  client) throws IOException {
        // TODO: Implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}

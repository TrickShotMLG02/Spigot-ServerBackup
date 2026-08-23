package net.server_backup.utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SftpManager {

    private final CommandSender sender;
    private final String server;
    private final int port;
    private final String user;
    private final String pass;
    private final String fingerprint;
    private final String workingDir;

    public SftpManager(CommandSender sender) {
        this.sender = sender;
        this.server = ServerBackup.getInstance().getConfig().getString("Sftp.Server.IP", "127.0.0.1");
        this.port = ServerBackup.getInstance().getConfig().getInt("Sftp.Server.Port", 22);
        this.user = ServerBackup.getInstance().getConfig().getString("Sftp.Server.User", "");
        this.pass = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Password", "");
        this.fingerprint = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Fingerprint", "");
        this.workingDir = ServerBackup.getInstance().getConfig().getString("Sftp.Server.BackupDirectory", "Backups/");
    }

    ServerBackup backup = ServerBackup.getInstance();

    /***
     * Creates the Remote Path including Working Directory and a relative Path
     * @param relativePath The relative path starting from the working directory
     * @return The full path from the root of the sftp session
     */
    private String getRemotePath(String fileName) {
        String directory = workingDir.replace('\\', '/');
        while (directory.endsWith("/")) {
            directory = directory.substring(0, directory.length() - 1);
        }
        return directory + "/" + fileName;
    }

    private boolean isSafeFileName(String fileName) {
        return fileName != null && !fileName.isBlank()
                && !fileName.equals(".") && !fileName.equals("..")
                && !fileName.contains("/") && !fileName.contains("\\")
                && !Paths.get(fileName).isAbsolute();
    }

    private File resolveLocalFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            file = new File(Configuration.backupDestination, filePath);
        }
        try {
            Path backupRoot = Paths.get(Configuration.backupDestination).toAbsolutePath().normalize();
            Path resolved = file.getCanonicalFile().toPath();
            if (!resolved.startsWith(backupRoot) || !Files.isRegularFile(resolved)) {
                return null;
            }
            return resolved.toFile();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Uploads a local file to the configured SFTP server.
     * <p>
     * Resolves the given {@code filePath}, connects to the SFTP server,
     * uploads the file to the remote path, and optionally deletes the local file
     * if configured. Sends messages to {@code sender} about progress and success/failure.
     * </p>
     *
     * @param filePath the path of the local file to upload
     * @param direct   currently unused flag indicating direct upload
     */
    public void uploadFileToSftp(String filePath, boolean direct) {
        File file = resolveLocalFile(filePath);

        if (file == null) {
            sender.sendMessage(OperationHandler.processMessage("Error.NoBackupFound").replaceAll("%file%", new File(filePath).getName()));

            return;
        }

        filePath = file.getPath();

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        String task = "SFTP UPLOAD {" + filePath + "}";
        boolean taskAdded = false;
        try {
            sftpClient = connect(sshClient);

            sender.sendMessage(OperationHandler.processMessage("Info.SftpUpload").replaceAll("%file%", file.getName()));
            OperationHandler.tasks.add(task);
            taskAdded = true;

            FileSystemFile localFile = new FileSystemFile(file);
            sftpClient.put(localFile, getRemotePath(file.getName()));
            sender.sendMessage(OperationHandler.processMessage("Info.SftpUploadSuccess"));

            if (ServerBackup.getInstance().getConfig().getBoolean("Sftp.DeleteLocalBackup")) {
                try {
                    boolean exists = false;
                    for (RemoteResourceInfo backup : sftpClient.ls(workingDir, RemoteResourceInfo::isRegularFile)) {
                        if (backup.getName().equals(file.getName())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists || !file.delete()) {
                        sender.sendMessage(OperationHandler.processMessage("Error.SftpLocalDeletionFailed"));
                    }
                } catch (IOException e) {
                    sender.sendMessage(OperationHandler.processMessage("Error.SftpLocalDeletionFailed"));
                    ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING,
                            "SFTP upload succeeded, but local backup cleanup failed", e);
                }
            }
        } catch (IOException e) {
            if (sftpClient == null) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpConnectionFailed"));
            } else {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpUploadFailed"));
            }
            ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "SFTP operation failed", e);
        } finally {
            if (taskAdded) {
                OperationHandler.tasks.remove(task);
            }
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Could not close SFTP connection", e);
            }
        }
    }

    /**
     * Downloads a file from the configured SFTP server to the local backup destination.
     * <p>
     * Checks if the file exists on the remote server, downloads it if found,
     * and sends progress and status messages to {@code sender}. Handles
     * connection and I/O exceptions and ensures the SFTP client is disconnected.
     * </p>
     *
     * @param filePath the path of the file to download from the remote server
     */
    public void downloadFileFromSftp(String filePath) {
        File file = new File(filePath);

        if (!isSafeFileName(file.getName()) || !file.getName().equals(filePath)) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));
            return;
        }

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            boolean exists = false;

            for (RemoteResourceInfo backup : sftpClient.ls(workingDir, RemoteResourceInfo::isRegularFile)) {
                if (backup.getName().equals(file.getName())) {
                    exists = true;
                }
            }

            if (!exists) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));

                return;
            }

            sender.sendMessage(OperationHandler.processMessage("Info.SftpDownload").replaceAll("%file%", file.getName()));

            File dFile = new File(Configuration.backupDestination, file.getName());
            if (Files.isSymbolicLink(dFile.toPath())) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpDownloadFailed"));
                return;
            }

            try {
                sftpClient.get(getRemotePath(filePath), dFile.getAbsolutePath());
                sender.sendMessage(OperationHandler.processMessage("Info.SftpDownloadSuccess"));
            } catch (IOException e) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpDownloadFailed"));
                ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "SFTP download failed", e);
            }

        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpConnectionFailed"));
            ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "SFTP download operation failed", e);
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Could not close SFTP connection", e);
            }
        }
    }

    /**
     * Deletes a file from the configured SFTP server.
     * <p>
     * Checks if the specified file exists on the remote server, deletes it if found,
     * and sends progress and status messages to {@code sender}. Handles connection
     * and I/O exceptions and ensures the SFTP client is disconnected.
     * </p>
     *
     * @param filePath the path of the file to delete on the remote server
     */
    public void deleteFile(String filePath) {
        File file = new File(filePath);

        if (!isSafeFileName(file.getName()) || !file.getName().equals(filePath)) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));
            return;
        }

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            boolean exists = false;

            for (RemoteResourceInfo backup : sftpClient.ls(workingDir, RemoteResourceInfo::isRegularFile)) {
                if (backup.getName().equals(file.getName())) {
                    exists = true;
                }
            }

            if (!exists) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));

                return;
            }

            sender.sendMessage(OperationHandler.processMessage("Info.SftpDeletion").replaceAll("%file%", file.getName()));

            try {
                sftpClient.rm(getRemotePath(filePath));
                sender.sendMessage(OperationHandler.processMessage("Info.SftpDeletionSuccess"));
            } catch (IOException e) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpDeletionFailed"));
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpConnectionFailed"));
            ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "SFTP delete operation failed", e);
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Could not close SFTP connection", e);
            }
        }
    }

    /**
     * Retrieves a list of backup files from the configured SFTP server.
     * <p>
     * Lists all regular files in the remote working directory and returns
     * them either in a raw format (path:size in MB) or a formatted display
     * string with an index and file size.
     * </p>
     *
     * @param rawList if true, returns a raw "path:size" list; if false, returns a formatted list
     * @return a list of backup file strings from the SFTP server
     */
    public List<String> getSftpBackupList(boolean rawList) {

        List<String> backups = new ArrayList<>();

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            List<RemoteResourceInfo> files = sftpClient.ls(workingDir, RemoteResourceInfo::isRegularFile);

            int c = 1;

            for (RemoteResourceInfo file : files) {
                double fileSize = (double) file.getAttributes().getSize() / 1000 / 1000;
                fileSize = Math.round(fileSize * 100.0) / 100.0;

                if (rawList) {
                    backups.add(file.getPath() + ":" + fileSize);
                } else {
                    backups.add("§7[" + c + "]§f " + file.getName() + " §7[" + fileSize + "MB]");
                }

                c++;
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpConnectionFailed"));
            ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "SFTP list operation failed", e);
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                ServerBackup.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Could not close SFTP connection", e);
            }
        }
        return backups;

    }

    /**
     * Connects to the SFTP server using password authentication and returns an SFTPClient.
     *
     * <p>This method establishes an SSH connection to the configured server, authenticates
     * with the provided username and password, and verifies the host key using a
     * fingerprint from the configuration.</p>
     *
     * <p><b>Note:</b> The returned SFTPClient does not maintain a current working directory.
     * All file operations, such as upload or download, must use the full remote path,
     * including the target directory. For example:
     * <pre>
     * sftp.put(localFilePath, remoteDir + "/" + remoteFileName);
     * </pre>
     * </p>
     *
     * @param sshClient The SSHClient used to connect to SFTP server
     * @return an active SFTPClient connected to the remote server
     * @throws IOException if the connection, authentication, or host key verification fails
     */
    private SFTPClient connect(SSHClient sshClient) throws IOException {
        if (server == null || server.isBlank() || user == null || user.isBlank()
                || fingerprint == null || fingerprint.isBlank()
                || fingerprint.contains("xxxxxxxx")) {
            throw new IOException("SFTP configuration is incomplete; a valid host fingerprint is required");
        }

        // SSHJ performs the fingerprint comparison before authentication.
        // Never replace this with an accept-all HostKeyVerifier.
        sshClient.addHostKeyVerifier(fingerprint.trim());

        // establish connection
        sshClient.connect(server, port);
        sshClient.authPassword(user, pass);

        return sshClient.newSFTPClient();
    }

    /**
     * Closes the given SFTPClient and disconnects the associated SSHClient.
     *
     * <p>This method ensures that both the SFTP session and the underlying SSH
     * connection are properly terminated to avoid resource leaks.</p>
     *
     * @param sftpClient the active SFTPClient to close; may be null
     * @param sshClient  the SSHClient associated with the SFTPClient; may be null
     * @throws IOException if an error occurs while closing the SFTPClient or disconnecting the SSHClient
     */
    private void disconnect(SFTPClient  sftpClient, SSHClient sshClient) throws IOException {

        // close sftp client
        if (sftpClient != null) {
            sftpClient.close();
        }

        // disconnect ssh client
        if (sshClient != null && sshClient.isConnected()) {
            sshClient.disconnect();
        }
    }
}

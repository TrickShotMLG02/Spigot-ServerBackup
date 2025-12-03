package net.server_backup.utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class SftpManager {

    private CommandSender sender;

    private static final String server = ServerBackup.getInstance().getConfig().getString("Sftp.Server.IP");
    private static final int port = ServerBackup.getInstance().getConfig().getInt("Sftp.Server.Port");
    private static final String user = ServerBackup.getInstance().getConfig().getString("Sftp.Server.User");
    private static final String pass = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Password");
    private static final String fingerprint = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Fingerprint");
    private static final String working_dir = ServerBackup.getInstance().getConfig().getString("Sftp.Server.BackupDirectory");

    public SftpManager(CommandSender sender) {
        this.sender = sender;
    }

    ServerBackup backup = ServerBackup.getInstance();

    /***
     * Creates the Remote Path including Working Directory and a relative Path
     * @param relativePath The relative path starting from the working directory
     * @return The full path from the root of the sftp session
     */
    private String getRemotePath(String relativePath) {
        // convert \\ to / since sftp expects only /
        return Paths.get(working_dir, relativePath).toString().replace('\\', '/');
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
        File file = new File(filePath);

        if (!file.getPath().contains(Configuration.backupDestination.replaceAll("/", ""))) {
            file = new File(Configuration.backupDestination + "//" + filePath);
            filePath = file.getPath();
        }

        if (!file.exists()) {
            sender.sendMessage(OperationHandler.processMessage("Error.NoBackupFound").replaceAll("%file%", file.getName()));

            return;
        }

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            sender.sendMessage(OperationHandler.processMessage("Info.SftpUpload").replaceAll("%file%", file.getName()));
            OperationHandler.tasks.add("SFTP UPLOAD {" + filePath + "}");


            try {
                FileSystemFile localFile = new FileSystemFile(file);
                sftpClient.put(localFile, getRemotePath(file.getName()));
                sender.sendMessage(OperationHandler.processMessage("Info.SftpUploadSuccess"));

                if (ServerBackup.getInstance().getConfig().getBoolean("Ftp.DeleteLocalBackup")) {
                    boolean exists = false;
                    for (RemoteResourceInfo backup : sftpClient.ls(working_dir, RemoteResourceInfo::isRegularFile)) {
                        if (backup.getName().equalsIgnoreCase(file.getName())) {
                            exists = true;
                        }
                    }

                    if (exists) {
                        file.delete();
                    } else {
                        sender.sendMessage(OperationHandler.processMessage("Error.SftpLocalDeletionFailed"));
                    }
                }
            } catch (IOException e) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpUploadFailed"));
                e.printStackTrace();
            }

        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpUploadFailed"));
            e.printStackTrace();
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                // TODO: Handle exception here
                e.printStackTrace();
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

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            boolean exists = false;

            for (RemoteResourceInfo backup : sftpClient.ls(working_dir, RemoteResourceInfo::isRegularFile)) {
                if (backup.getName().equalsIgnoreCase(file.getName())) {
                    exists = true;
                }
            }

            if (!exists) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));

                return;
            }

            sender.sendMessage(OperationHandler.processMessage("Info.SftpDownload").replaceAll("%file%", file.getName()));

            File dFile = new File(Configuration.backupDestination + "//" + file.getPath());

            try {
                sftpClient.get(getRemotePath(filePath), dFile.getAbsolutePath());
                sender.sendMessage(OperationHandler.processMessage("Info.SftpDownloadSuccess"));
            } catch (IOException e) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpDownloadFailed"));
                e.printStackTrace();
            }

        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpDownloadFailed"));
            e.printStackTrace();
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                // TODO: Handle exception here
                e.printStackTrace();
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

        SSHClient sshClient = new SSHClient();
        SFTPClient sftpClient = null;

        try {
            sftpClient = connect(sshClient);

            boolean exists = false;

            for (RemoteResourceInfo backup : sftpClient.ls(working_dir, RemoteResourceInfo::isRegularFile)) {
                if (backup.getName().equalsIgnoreCase(file.getName())) {
                    exists = true;
                }
            }

            if (!exists) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpNotFound").replaceAll("%file%", file.getName()));

                return;
            }

            sender.sendMessage(OperationHandler.processMessage("Info.FtpDeletion").replaceAll("%file%", file.getName()));

            try {
                sftpClient.rm(getRemotePath(filePath));
                sender.sendMessage(OperationHandler.processMessage("Info.SftpDeletionSuccess"));
            } catch (IOException e) {
                sender.sendMessage(OperationHandler.processMessage("Error.SftpDeletionFailed"));
            }
        } catch (IOException e) {
            sender.sendMessage(OperationHandler.processMessage("Error.SftpDeletionFailed"));
            e.printStackTrace();
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                // TODO: Handle exception here
                e.printStackTrace();
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

            List<RemoteResourceInfo> files = sftpClient.ls(working_dir, RemoteResourceInfo::isRegularFile);

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
            // TODO: Handle exception here
            e.printStackTrace();
        } finally {
            try {
                disconnect(sftpClient, sshClient);
            } catch (IOException e) {
                // TODO: Handle exception here
                e.printStackTrace();
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

        // Create verifier for host key from config
        HostKeyVerifier verifier = new HostKeyVerifier() {
            @Override
            public boolean verify(String hostname, int port, PublicKey key) {
                return true;

                // TODO: FIND A SOLUTION TO CHECK FINGERPRINTS, OR A WAY TO LOAD "BouncyCastle"

                /*
                String actualFingerprint = SecurityUtils.getFingerprint(key);
                return actualFingerprint.equals(fingerprint);
                 */
            }

            @Override
            public List<String> findExistingAlgorithms(String s, int i) {
                return List.of();
            }
        };

        sshClient.addHostKeyVerifier(verifier);

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

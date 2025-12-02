package net.server_backup.utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.server_backup.ServerBackup;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

public class SftpManager {

    private CommandSender sender;

    private static final String server = ServerBackup.getInstance().getConfig().getString("Sftp.Server.IP");
    private static final int port = ServerBackup.getInstance().getConfig().getInt("Sftp.Server.Port");
    private static final String user = ServerBackup.getInstance().getConfig().getString("Sftp.Server.User");
    private static final String pass = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Password");
    private static final String fingerprint = ServerBackup.getInstance().getConfig().getString("Sftp.Server.Fingerprint");

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
                String actualFingerprint = SecurityUtils.getFingerprint(key);
                return actualFingerprint.equals(fingerprint);
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

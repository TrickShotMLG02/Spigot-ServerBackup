package net.server_backup.core.storage;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.xfer.FileSystemFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Password-authenticated SFTP storage with mandatory host-key fingerprint verification. */
public final class SftpStorage implements RemoteStorage {
    private final String host, user, password, fingerprint, directory;
    private final int port;
    public SftpStorage(String host, int port, String user, String password, String fingerprint, String directory) {
        this.host = host; this.port = port; this.user = user; this.password = password;
        this.fingerprint = fingerprint; this.directory = directory;
    }
    private SSHClient connect() throws IOException {
        if (host == null || host.isBlank() || user == null || user.isBlank() || fingerprint == null || fingerprint.isBlank() || fingerprint.contains("xxxxxxxx"))
            throw new IOException("SFTP configuration is incomplete; a valid host fingerprint is required");
        SSHClient client = new SSHClient();
        try {
            client.setConnectTimeout(10000);
            client.setTimeout(30000);
            client.addHostKeyVerifier(fingerprint.trim()); client.connect(host, port); client.authPassword(user, password);
            return client;
        } catch (IOException | RuntimeException e) { try { client.close(); } catch (IOException ignored) { } throw e; }
    }
    private String remote(String name) {
        if (name == null || name.isBlank() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\"))
            throw new IllegalArgumentException("Invalid remote filename");
        return directory.replace('\\', '/').replaceAll("/+$", "") + "/" + name;
    }
    public List<RemoteResourceInfo> list() throws IOException {
        try (SSHClient ssh = connect(); SFTPClient sftp = ssh.newSFTPClient()) { return sftp.ls(directory, RemoteResourceInfo::isRegularFile); }
    }
    public List<RemoteFile> listFiles() throws IOException {
        java.util.ArrayList<RemoteFile> files = new java.util.ArrayList<>();
        for (RemoteResourceInfo entry : list()) files.add(new RemoteFile(entry.getName(), entry.getPath(), entry.getAttributes().getSize()));
        return files;
    }
    public boolean store(Path source) throws IOException { upload(source); return true; }
    public boolean fetch(String name, Path destination) throws IOException {
        String path = remote(name);
        return SafeDownload.run(destination, temporary -> {
            try (SSHClient ssh = connect(); SFTPClient sftp = ssh.newSFTPClient()) {
                if (sftp.statExistence(path) == null) return false;
                sftp.get(path, temporary.toString());
                return true;
            }
        });
    }
    public void upload(Path source) throws IOException {
        try (SSHClient ssh = connect(); SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.getFileTransfer().setPreserveAttributes(false);
            sftp.put(new FileSystemFile(source.toFile()), remote(source.getFileName().toString()));
        }
    }
    public void download(String name, Path destination) throws IOException {
        if (!fetch(name, destination)) throw new java.io.FileNotFoundException(name);
    }
    public void delete(String name) throws IOException {
        String path = remote(name);
        try (SSHClient ssh = connect(); SFTPClient sftp = ssh.newSFTPClient()) { sftp.rm(path); }
    }
}

package net.server_backup.core.storage;

import org.apache.commons.net.ftp.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** FTP transport. Explicit FTPS is tried first, falling back to the legacy plain FTP mode. */
public final class FtpStorage implements RemoteStorage {
    private final String host, user, password, directory;
    private final int port;
    public FtpStorage(String host, int port, String user, String password, String directory) {
        this.host = host; this.port = port; this.user = user; this.password = password; this.directory = directory;
    }
    private FTPClient connect() throws IOException {
        FTPClient secure = new FTPSClient();
        try { configure(secure); return secure; }
        catch (IOException secureFailure) {
            close(secure);
            FTPClient plain = new FTPClient();
            try { configure(plain); return plain; }
            catch (IOException failure) { close(plain); failure.addSuppressed(secureFailure); throw failure; }
        }
    }
    private void configure(FTPClient client) throws IOException {
        client.setConnectTimeout(10000);
        client.setDefaultTimeout(30000);
        client.setDataTimeout(java.time.Duration.ofSeconds(30));
        client.connect(host, port);
        client.setSoTimeout(30000);
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()) || !client.login(user, password)) throw new IOException("FTP login failed");
        client.enterLocalPassiveMode();
        if (client instanceof FTPSClient) { ((FTPSClient) client).execPBSZ(0); ((FTPSClient) client).execPROT("P"); }
        if (!client.setFileType(FTP.BINARY_FILE_TYPE)) throw new IOException("FTP binary mode failed");
        if (!client.changeWorkingDirectory(directory)) throw new IOException("FTP directory unavailable");
    }
    private void close(FTPClient client) {
        if (client.isConnected()) { try { client.logout(); } catch (IOException ignored) { } try { client.disconnect(); } catch (IOException ignored) { } }
    }
    public List<FTPFile> list() throws IOException {
        FTPClient client = connect();
        try { return Arrays.asList(client.listFiles()); } finally { close(client); }
    }
    public List<RemoteFile> listFiles() throws IOException {
        List<RemoteFile> files = new ArrayList<>();
        for (FTPFile entry : list()) files.add(new RemoteFile(entry.getName(), entry.getName(), entry.getSize()));
        return files;
    }
    public boolean store(Path source) throws IOException { return upload(source); }
    public boolean fetch(String name, Path destination) throws IOException { return download(name, destination); }
    public boolean upload(Path source) throws IOException {
        FTPClient client = connect();
        try (InputStream in = Files.newInputStream(source)) { return client.storeFile(source.getFileName().toString(), in); }
        finally { close(client); }
    }
    public boolean download(String name, Path destination) throws IOException {
        validateName(name);
        return SafeDownload.run(destination, temporary -> {
            FTPClient client = connect();
            try (OutputStream out = Files.newOutputStream(temporary)) { return client.retrieveFile(name, out); }
            finally { close(client); }
        });
    }
    public boolean delete(String name) throws IOException {
        validateName(name); FTPClient client = connect();
        try { return client.deleteFile(name); } finally { close(client); }
    }
    private static void validateName(String name) {
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) throw new IllegalArgumentException("Invalid remote filename");
    }
}

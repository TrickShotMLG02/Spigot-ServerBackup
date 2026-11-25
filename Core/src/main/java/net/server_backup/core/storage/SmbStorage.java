package net.server_backup.core.storage;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** SMB storage independent of any server API. */
public final class SmbStorage implements RemoteStorage {
    private final String host, share, directory, domain, user, password;
    private final int port;
    public SmbStorage(String host, int port, String share, String directory, String domain, String user, String password) {
        this.host = host.trim(); this.port = port; this.share = share.trim(); this.directory = directory.trim();
        this.domain = domain; this.user = user; this.password = password;
        if (!this.host.matches("[A-Za-z0-9._:-]+") || !this.share.matches("[A-Za-z0-9_$.-]+") || port < 1 || port > 65535
                || this.directory.startsWith("/") || this.directory.contains("..") || this.directory.contains("\\"))
            throw new IllegalArgumentException("Invalid SMB endpoint or directory");
    }
    private SmbFile folder() throws IOException {
        CIFSContext base = SingletonContext.getInstance();
        CIFSContext context = base.withCredentials(new NtlmPasswordAuthentication(base, domain, user, password));
        return new SmbFile("smb://" + host + ":" + port + "/" + share + "/"
                + (directory.isEmpty() ? "" : directory.replaceAll("/+$", "") + "/"), context);
    }
    private static void validateName(String name) {
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals(".."))
            throw new IllegalArgumentException("Invalid remote filename");
    }
    public void upload(Path source) throws IOException {
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) throw new IOException("Source is not readable");
        SmbFile folder = folder(); folder.mkdirs();
        SmbFile remote = new SmbFile(folder, source.getFileName().toString());
        try (InputStream in = Files.newInputStream(source); OutputStream out = new SmbFileOutputStream(remote)) { in.transferTo(out); }
        if (remote.length() != Files.size(source)) throw new IOException("Remote size verification failed");
    }
    public List<String> list() throws IOException {
        List<String> result = new ArrayList<>();
        for (RemoteFile remote : listFiles()) result.add(remote.name + " " + (remote.size / 1000000.0) + "MB");
        return result;
    }
    public List<RemoteFile> listFiles() throws IOException {
        List<RemoteFile> result = new ArrayList<>();
        for (SmbFile remote : folder().listFiles()) if (remote.isFile()) result.add(new RemoteFile(remote.getName(), remote.getName(), remote.length()));
        return result;
    }
    public boolean store(Path source) throws IOException { upload(source); return true; }
    public boolean fetch(String name, Path destination) throws IOException { return download(name, destination); }
    public boolean download(String name, Path destination) throws IOException {
        validateName(name);
        SmbFile remote = new SmbFile(folder(), name);
        if (!remote.isFile()) return false;
        Path absolute = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(absolute.getParent(), ".smb-", ".tmp");
        try {
            try (InputStream in = new SmbFileInputStream(remote); OutputStream out = Files.newOutputStream(temporary)) { in.transferTo(out); }
            if (Files.size(temporary) != remote.length()) throw new IOException("Downloaded size verification failed");
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

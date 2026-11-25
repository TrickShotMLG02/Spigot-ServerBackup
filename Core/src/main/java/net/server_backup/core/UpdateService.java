package net.server_backup.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Fetches update information and stages downloads without modifying the running jar. */
public final class UpdateService {
    private URLConnection connection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        return connection;
    }
    public String latest(URL url) throws IOException {
        try (InputStream input = connection(url).getInputStream()) {
            return new String(input.readNBytes(256), StandardCharsets.UTF_8).trim().split("\\s+")[0];
        }
    }
    public boolean isNewer(String latest, String current) {
        String[] left = latest.split("\\."), right = current.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? Integer.parseInt(left[i]) : 0;
            int r = i < right.length ? Integer.parseInt(right[i]) : 0;
            if (l != r) return l > r;
        }
        return false;
    }
    public void download(URL url, Path destination) throws IOException {
        Path target = destination.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".serverbackup-update-", ".tmp");
        try {
            try (InputStream input = connection(url).getInputStream()) { Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING); }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}

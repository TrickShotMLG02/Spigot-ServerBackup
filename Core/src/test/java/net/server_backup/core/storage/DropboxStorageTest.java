package net.server_backup.core.storage;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class DropboxStorageTest {
    @TempDir Path directory;
    private static final class FakeHttp extends HttpRequestor {
        final List<String> urls = new ArrayList<>(), arguments = new ArrayList<>();
        final List<Long> sizes = new ArrayList<>();
        boolean failUploads;
        public Response doGet(String url, Iterable<Header> headers) { throw new AssertionError("Unexpected GET"); }
        public Uploader startPut(String url, Iterable<Header> headers) { throw new AssertionError("Unexpected PUT"); }
        public Uploader startPost(String url, Iterable<Header> headers) {
            urls.add(url);
            for (Header header : headers) if (header.getKey().equalsIgnoreCase("Dropbox-API-Arg")) arguments.add(header.getValue());
            return new Uploader() {
                long count;
                final OutputStream sink = new OutputStream() {
                    public void write(int b) { count++; if (progressListener != null) progressListener.onProgress(count); }
                    public void write(byte[] bytes, int offset, int length) { count += length; if (progressListener != null) progressListener.onProgress(count); }
                };
                public OutputStream getBody() { return sink; }
                public void close() { }
                public void abort() { }
                public Response finish() {
                    sizes.add(count);
                    if (failUploads) return new Response(500, new ByteArrayInputStream("error".getBytes(StandardCharsets.UTF_8)), Map.of("x-dropbox-request-id", List.of("fixture")));
                    String body = url.endsWith("/upload_session/start") ? "{\"session_id\":\"session\"}"
                            : url.endsWith("/upload_session/append_v2") ? "null"
                            : "{\"name\":\"backup.zip\",\"id\":\"id:123\",\"client_modified\":\"2026-01-01T00:00:00Z\",\"server_modified\":\"2026-01-01T00:00:00Z\",\"rev\":\"123456789\",\"size\":4}";
                    return new Response(200, new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), Map.of("x-dropbox-request-id", List.of("fixture")));
                }
            };
        }
    }
    @Test void propagatesRemoteFailureWithoutDeletingSource() throws Exception {
        FakeHttp http = new FakeHttp(); http.failUploads = true;
        DropboxStorage storage = new DropboxStorage(new DbxClientV2(DbxRequestConfig.newBuilder("test").withHttpRequestor(http).withAutoRetryDisabled().build(), "token"));
        Path file = Files.writeString(directory.resolve("backup.zip"), "data");
        assertThrows(com.dropbox.core.DbxException.class, () -> storage.upload(file, "/"));
        assertEquals("data", Files.readString(file));
    }
    @Test void uploadsSmallFileOnceToTheExpectedPath() throws Exception {
        FakeHttp http = new FakeHttp();
        DropboxStorage storage = new DropboxStorage(new DbxClientV2(DbxRequestConfig.newBuilder("test").withHttpRequestor(http).build(), "token"));
        Path file = Files.writeString(directory.resolve("backup.zip"), "data");
        storage.upload(file, "/archives/");
        assertEquals(List.of(4L), http.sizes);
        assertEquals(1, http.urls.size());
        assertTrue(http.arguments.get(0).contains("/archives/backup.zip"));
    }
    @Test void splitsLargeFilesIntoBoundedUploadSessionChunks() throws Exception {
        FakeHttp http = new FakeHttp();
        DropboxStorage storage = new DropboxStorage(new DbxClientV2(DbxRequestConfig.newBuilder("test").withHttpRequestor(http).build(), "token"));
        Path file = directory.resolve("large.zip");
        try (RandomAccessFile output = new RandomAccessFile(file.toFile(), "rw")) { output.setLength(100_000_001L); }
        AtomicLong progress = new AtomicLong();
        storage.upload(file, "/", progress::set);
        assertTrue(http.urls.get(0).endsWith("/upload_session/start"));
        assertTrue(http.urls.get(http.urls.size() - 1).endsWith("/upload_session/finish"));
        assertEquals(100_000_001L, http.sizes.stream().mapToLong(Long::longValue).sum());
        assertTrue(http.sizes.stream().allMatch(size -> size <= (16L << 20)));
        assertEquals(100_000_001L, progress.get());
    }
}

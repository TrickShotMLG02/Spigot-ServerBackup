package net.server_backup.core.storage;

import com.dropbox.core.*;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** Dropbox authorization and upload, independent of server APIs. */
public final class DropboxStorage {
    private static final long CHUNK = 16L << 20;
    private final DbxClientV2 client;
    DropboxStorage(DbxClientV2 client) { this.client = client; }
    public DropboxStorage(String key, String secret, String code, String refreshToken, Consumer<String> saveRefreshToken) throws DbxException {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/ServerBackupG").build();
        DbxCredential credential;
        if (refreshToken == null || refreshToken.isEmpty()) {
            DbxAuthFinish auth = new DbxWebAuth(config, new DbxAppInfo(key, secret)).finishFromCode(code);
            credential = new DbxCredential(auth.getAccessToken(), 60L, auth.getRefreshToken(), key, secret);
            saveRefreshToken.accept(auth.getRefreshToken());
        } else credential = new DbxCredential("isly", 0L, refreshToken, key, secret);
        client = new DbxClientV2(config, credential);
    }
    public void upload(Path file, String folder) throws IOException, DbxException {
        upload(file, folder, ignored -> { });
    }
    public void upload(Path file, String folder, LongConsumer progress) throws IOException, DbxException {
        long size = Files.size(file);
        String destination = "/" + folder.replace("/", "") + (folder.replace("/", "").isEmpty() ? "" : "/") + file.getFileName();
        if (size <= 100_000_000L) {
            try (InputStream in = Files.newInputStream(file)) {
                client.files().uploadBuilder(destination).uploadAndFinish(in, progress::accept);
            }
            return;
        }
        String session = null;
        long uploaded = 0;
        DbxException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try (InputStream in = Files.newInputStream(file)) {
                in.skipNBytes(uploaded);
                if (session == null) {
                    session = client.files().uploadSessionStart().uploadAndFinish(in, CHUNK, progress::accept).getSessionId();
                    uploaded = CHUNK;
                }
                while (size - uploaded > CHUNK) {
                    long offset = uploaded;
                    client.files().uploadSessionAppendV2(new UploadSessionCursor(session, uploaded)).uploadAndFinish(in, CHUNK, n -> progress.accept(offset + n));
                    uploaded += CHUNK;
                }
                CommitInfo commit = CommitInfo.newBuilder(destination).withMode(WriteMode.ADD)
                        .withClientModified(new Date(Files.getLastModifiedTime(file).toMillis())).build();
                long offset = uploaded;
                client.files().uploadSessionFinish(new UploadSessionCursor(session, uploaded), commit)
                        .uploadAndFinish(in, size - uploaded, n -> progress.accept(offset + n));
                return;
            } catch (UploadSessionAppendErrorException e) {
                if (!e.errorValue.isIncorrectOffset()) throw e;
                uploaded = e.errorValue.getIncorrectOffsetValue().getCorrectOffset();
                if (uploaded < 0 || uploaded > size) throw e;
                lastFailure = e;
            } catch (UploadSessionFinishErrorException e) {
                if (!e.errorValue.isLookupFailed() || !e.errorValue.getLookupFailedValue().isIncorrectOffset()) throw e;
                uploaded = e.errorValue.getLookupFailedValue().getIncorrectOffsetValue().getCorrectOffset();
                if (uploaded < 0 || uploaded > size) throw e;
                lastFailure = e;
            } catch (RetryException | NetworkIOException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }
}

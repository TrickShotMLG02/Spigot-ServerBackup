package net.server_backup.core.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Common upload, download and listing capabilities of remote backup stores. */
public interface RemoteStorage {
    boolean store(Path file) throws IOException;
    boolean fetch(String name, Path destination) throws IOException;
    List<RemoteFile> listFiles() throws IOException;
}

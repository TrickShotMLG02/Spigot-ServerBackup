package net.server_backup.core.storage;

/** Platform-neutral remote backup metadata. */
public final class RemoteFile {
    public final String name, path;
    public final long size;
    public RemoteFile(String name, String path, long size) { this.name = name; this.path = path; this.size = size; }
}

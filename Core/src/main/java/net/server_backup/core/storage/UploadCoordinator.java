package net.server_backup.core.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/** Runs all configured uploads before applying any local deletion policy. */
public final class UploadCoordinator {
    public interface Target {
        boolean upload(Path file);
        boolean deleteLocalAfterSuccess();
    }
    public static Target target(Predicate<Path> upload, boolean deleteLocal) {
        return new Target() {
            public boolean upload(Path file) { return upload.test(file); }
            public boolean deleteLocalAfterSuccess() { return deleteLocal; }
        };
    }
    public boolean upload(Path file, List<Target> targets) throws IOException {
        boolean allSucceeded = true;
        boolean deleteLocal = false;
        for (Target target : targets) {
            boolean succeeded = target.upload(file);
            allSucceeded &= succeeded;
            deleteLocal |= target.deleteLocalAfterSuccess();
        }
        if (allSucceeded && deleteLocal && !targets.isEmpty()) Files.delete(file);
        return allSucceeded;
    }
}

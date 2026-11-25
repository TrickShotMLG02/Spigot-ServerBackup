package net.server_backup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/** Stores individually selected files in the legacy Files directory. */
public final class LocalFileBackup {
    public Path copy(Path source, Path backupDirectory, Clock clock) throws IOException {
        Path files = backupDirectory.resolve("Files");
        Files.createDirectories(files);
        String name = source.getFileName().toString();
        Path target = files.resolve(name);
        if (Files.exists(target)) {
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            String extension = dot > 0 ? name.substring(dot) : "";
            String suffix = " " + clock.instant().getEpochSecond();
            target = files.resolve(base + suffix + extension);
            int index = 1;
            while (Files.exists(target)) target = files.resolve(base + suffix + "-" + index++ + extension);
        }
        return Files.copy(source, target);
    }
}

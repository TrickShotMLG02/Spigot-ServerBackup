package net.server_backup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/** Coordinates backup naming and local removal through a platform-provided port. */
public final class BackupService {
    public interface Port {
        void executeAsync(Runnable task);
        /** Called before archiving; platform implementations may require the server thread. */
        void prepareBackup(String source);
        void backupStarted(String name);
        void archive(Path source, Path destination, boolean full) throws IOException;
        void message(String code, String name);
        void failure(String operation, Exception exception);
    }

    private final Path destination;
    private final Clock clock;
    private final Port port;

    public BackupService(Path destination, Clock clock, Port port) {
        this.destination = destination;
        this.clock = clock;
        this.port = port;
    }

    public void create(String name, boolean full) {
        boolean wholeServer = name.equalsIgnoreCase("@server");
        Path source = Paths.get(wholeServer ? "." : name);
        String archiveName = wholeServer ? "server" : name;
        if (!Files.exists(source)) return;
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'~'HH-mm-ss"));
        String safeName = archiveName.replace('/', '-').replace('\\', '-').replace(':', '-');
        Path target = destination.resolve("backup-" + timestamp + "-" + safeName + ".zip");
        try {
            port.prepareBackup(name);
            port.backupStarted(source.toFile().getName());
            port.archive(source, target, full);
        } catch (IOException | RuntimeException e) {
            port.failure("create", e);
        }
    }

    public void remove(String name) {
        port.executeAsync(() -> {
            Path target = destination.resolve(name).normalize();
            if (!target.toAbsolutePath().startsWith(destination.toAbsolutePath().normalize()) || target.equals(destination.normalize())) {
                port.message("Error.DeletionFailed", name);
                return;
            }
            if (!Files.exists(target)) {
                port.message("Error.NoBackupFound", name);
                return;
            }
            try {
                Path realRoot = destination.toRealPath();
                Path realTarget = target.toRealPath();
                if (!realTarget.startsWith(realRoot) || realTarget.equals(realRoot)) {
                    port.message("Error.DeletionFailed", name);
                    return;
                }
            } catch (IOException e) {
                port.failure("remove", e);
                port.message("Error.DeletionFailed", name);
                return;
            }
            try (Stream<Path> paths = Files.walk(target)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
                port.message("Info.BackupRemoved", name);
            } catch (IOException e) {
                port.failure("remove", e);
                port.message("Error.DeletionFailed", name);
            }
        });
    }
}

package net.server_backup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Platform-neutral archive selection, creation, extraction and upload orchestration. */
public final class ArchiveWorkflow {
    public interface Port {
        boolean isTrackedRegion(String key);
        void clearTrackedRegions(String source);
        void progress(Path path);
        void skipped(Path path);
        void upload(Path archive) throws IOException;
    }
    private final Port port;
    public ArchiveWorkflow(Port port) { this.port = port; }

    public Path create(Path source, Path target, Path backupDestination, List<String> blacklist,
                       boolean saving, boolean full, boolean dynamic) throws IOException {
        Path root = backupDestination.toAbsolutePath().normalize();
        new ArchiveCreator().create(source, target, path -> {
            Path absolute = path.toAbsolutePath().normalize();
            if (saving && absolute.startsWith(root)) return false;
            for (String item : blacklist) {
                Path excluded = Path.of(item).toAbsolutePath().normalize();
                if (absolute.equals(excluded) || (Files.isDirectory(excluded) && absolute.startsWith(excluded))) {
                    port.skipped(path);
                    return false;
                }
            }
            if (!full && dynamic && path.getParent() != null && path.getParent().getParent() != null) {
                String parent = path.getParent().getFileName().toString();
                if ((parent.equals("region") || parent.equals("entities") || parent.equals("poi"))
                        && !port.isTrackedRegion("Data." + path.getParent().getParent() + ".Chunk." + path.getFileName())) return false;
            }
            return true;
        }, port::progress);
        if (!saving) deleteTree(source);
        if (!full && dynamic && !source.toString().equals(".")) {
            String name = target.getFileName().toString();
            if (name.startsWith("backup-")) {
                Path dynamicTarget = target.resolveSibling("dynamic-" + name);
                Files.move(target, dynamicTarget, StandardCopyOption.REPLACE_EXISTING);
                target = dynamicTarget;
                port.clearTrackedRegions(source.toString());
            }
        }
        port.upload(target);
        return target;
    }
    public void extract(Path archive, Path target) throws IOException {
        new ArchiveExtractor().extract(archive, target, port::progress);
        Files.delete(archive);
    }
    private void deleteTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }
}

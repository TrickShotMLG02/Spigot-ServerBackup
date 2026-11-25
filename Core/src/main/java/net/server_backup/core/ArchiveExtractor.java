package net.server_backup.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extracts a ZIP into a directory, rejecting entries that escape it. */
public final class ArchiveExtractor {
    public void extract(Path archive, Path destination, Consumer<Path> progress) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        for (Path current = root; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IOException("Extraction destination crosses a symbolic link: " + current);
        }
        Files.createDirectories(root);
        try (InputStream input = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("Archive entry escapes destination: " + entry.getName());
                }
                // Existing symbolic links can otherwise redirect an apparently safe path.
                Path current = root;
                for (Path part : root.relativize(target)) {
                    current = current.resolve(part);
                    if (Files.isSymbolicLink(current)) {
                        throw new IOException("Archive entry crosses a symbolic link: " + entry.getName());
                    }
                }
                progress.accept(target);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }
}

package net.server_backup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates ZIP archives without any dependency on a server platform. */
public final class ArchiveCreator {
    public void create(Path source, Path destination, Predicate<Path> include, Consumer<Path> progress) throws IOException {
        Files.createFile(destination);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination, StandardOpenOption.WRITE));
             Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths.filter(path -> !Files.isDirectory(path))::iterator) {
                if (path.toAbsolutePath().normalize().equals(destination.toAbsolutePath().normalize()) || !include.test(path)) continue;
                progress.accept(path);
                zip.putNextEntry(new ZipEntry(source.relativize(path).toString().replace('\\', '/')));
                if (!(System.getProperty("os.name").startsWith("Windows") && path.toString().contains("session.lock"))) {
                    try (java.io.InputStream input = Files.newInputStream(path)) {
                        byte[] buffer = new byte[65536];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Archive creation interrupted");
                            zip.write(buffer, 0, count);
                        }
                    }
                }
                zip.closeEntry();
            }
        } catch (IOException | RuntimeException e) {
            try { Files.deleteIfExists(destination); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }
}

package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveExtractorTest {
    @TempDir Path directory;

    private Path archive(String name, String contents) throws IOException {
        Path archive = directory.resolve("input.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
    }

    @Test void extractsNestedFiles() throws IOException {
        Path archive = archive("world/level.dat", "data");
        Path output = directory.resolve("out");
        new ArchiveExtractor().extract(archive, output, ignored -> {});
        assertEquals("data", Files.readString(output.resolve("world/level.dat")));
    }

    @Test void rejectsParentTraversal() throws IOException {
        Path archive = archive("../escape", "data");
        assertThrows(IOException.class, () -> new ArchiveExtractor().extract(archive, directory.resolve("out"), ignored -> {}));
        assertFalse(Files.exists(directory.resolve("escape")));
    }

    @Test void rejectsSymlinkAsDestination() throws IOException {
        Path archive = archive("file", "data");
        Path real = Files.createDirectory(directory.resolve("real"));
        Path link = directory.resolve("link");
        try { Files.createSymbolicLink(link, real); }
        catch (UnsupportedOperationException | IOException | SecurityException e) {
            org.junit.jupiter.api.Assumptions.abort("Symbolic links are unavailable: " + e.getMessage());
        }
        assertThrows(IOException.class, () -> new ArchiveExtractor().extract(archive, link, ignored -> {}));
        assertFalse(Files.exists(real.resolve("file")));
    }
}

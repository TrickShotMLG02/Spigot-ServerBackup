package net.server_backup.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class ArchiveCreatorTest {
    @TempDir Path directory;

    @Test void createsArchiveAndRespectsFilter() throws IOException {
        Path source = Files.createDirectory(directory.resolve("source"));
        Files.writeString(source.resolve("included"), "contents");
        Files.writeString(source.resolve("excluded"), "secret");
        Path archive = source.resolve("backup.zip");
        new ArchiveCreator().create(source, archive, path -> !path.endsWith("excluded"), ignored -> {});
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertNotNull(zip.getEntry("included"));
            assertNull(zip.getEntry("excluded"));
            assertNull(zip.getEntry("backup.zip"));
            assertEquals("contents", new String(zip.getInputStream(zip.getEntry("included")).readAllBytes()));
        }
    }

    @Test void refusesToOverwriteAnExistingArchive() throws IOException {
        Path archive = Files.writeString(directory.resolve("existing.zip"), "original");
        assertThrows(IOException.class, () -> new ArchiveCreator().create(directory, archive, p -> true, p -> {}));
        assertEquals("original", Files.readString(archive));
    }

    @Test void removesIncompleteArchiveAfterFailure() {
        Path target = directory.resolve("partial.zip");
        assertThrows(IOException.class, () -> new ArchiveCreator().create(directory.resolve("missing"), target, p -> true, p -> {}));
        assertFalse(Files.exists(target));
    }

    @Test void interruptedCreationRemovesPartialOutput() throws IOException {
        Path source = Files.createDirectory(directory.resolve("source"));
        Files.writeString(source.resolve("data"), "contents");
        Path target = directory.resolve("partial.zip");
        Thread.currentThread().interrupt();
        try {
            assertThrows(IOException.class, () -> new ArchiveCreator().create(source, target, p -> true, p -> {}));
            assertFalse(Files.exists(target));
            assertTrue(Files.exists(source.resolve("data")));
        } finally { Thread.interrupted(); }
    }
}

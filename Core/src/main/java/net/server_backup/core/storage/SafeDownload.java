package net.server_backup.core.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stages a transfer so a failed download cannot replace an existing local file. */
final class SafeDownload {
    interface Transfer { boolean run(Path temporary) throws IOException; }
    static boolean run(Path destination, Transfer transfer) throws IOException {
        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(target.getParent(), ".serverbackup-download-", ".tmp");
        try {
            if (!transfer.run(temporary)) return false;
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally { Files.deleteIfExists(temporary); }
    }
}

package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompletionServiceTest {
    private final CompletionService service = new CompletionService(new CompletionService.Port() {
        public List<String> worlds() { return List.of("world", "world_nether"); }
        public List<String> localFiles() { return List.of("backup.zip", "folder"); }
        public List<String> localRegularFiles() { return List.of("backup.zip"); }
        public List<String> remoteFiles(String provider) { return List.of("remote.zip"); }
    });
    @Test void selectsAndFiltersCandidatesWithoutPlatformApis() {
        assertTrue(service.complete(false, new String[]{""}).isEmpty());
        assertEquals(List.of("backup.zip"), service.complete(true, new String[]{"ftp", "upload", "b"}));
        assertEquals(List.of("remote.zip"), service.complete(true, new String[]{"sftp", "download", "r"}));
        assertEquals(List.of("world", "world_nether"), service.complete(true, new String[]{"create", "w"}));
    }
}

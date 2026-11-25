package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OperationRegistryTest {
    @Test void snapshotsRemainStableWhenOperationsComplete() {
        OperationRegistry registry = new OperationRegistry();
        registry.add("create"); registry.add("upload");
        List<String> snapshot = registry.snapshot();
        registry.remove("create"); registry.remove("upload");
        assertEquals(List.of("create", "upload"), snapshot);
        assertTrue(registry.isEmpty());
    }
}

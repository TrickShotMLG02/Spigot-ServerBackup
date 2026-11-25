package net.server_backup.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShutdownRequestTest {
    @Test void togglesDeferredShutdown() {
        ShutdownRequest request = new ShutdownRequest();
        assertFalse(request.isRequested());
        assertTrue(request.toggle());
        assertTrue(request.isRequested());
        assertFalse(request.toggle());
        assertFalse(request.isRequested());
    }
}

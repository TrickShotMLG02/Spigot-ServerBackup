package net.server_backup.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateRequestTest {
    @Test void preservesNamesWithSpacesAndFullFlag() {
        CreateRequest request = CreateRequest.parse(new String[]{"create", "my", "-full", "world"});
        assertEquals("my world", request.source);
        assertTrue(request.full);
        assertFalse(CreateRequest.parse(new String[]{"create", "@server"}).isSingleFile());
        assertThrows(IllegalArgumentException.class, () -> CreateRequest.parse(new String[]{"create"}));
    }
}

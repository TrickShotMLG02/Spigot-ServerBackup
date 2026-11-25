package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessageCatalogTest {
    @Test void formatsAnImmutableSnapshot() {
        Map<String, String> source = new HashMap<>();
        source.put("message", "&aHello&nlworld");
        MessageCatalog catalog = new MessageCatalog("[Backup] ", source);
        source.put("message", "changed");
        assertEquals("[Backup] \u00a7aHello\nworld", catalog.format("message"));
    }
}

package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {
    @Test void snapshotsValuesAndListsIndependentlyOfSource() {
        List<String> worlds = new ArrayList<>(List.of("world"));
        Map<String, Object> source = new HashMap<>();
        source.put("worlds", worlds); source.put("enabled", true); source.put("port", 22);
        Settings settings = new Settings(source);
        worlds.add("new-world"); source.put("enabled", false);
        assertEquals(List.of("world"), settings.getStringList("worlds"));
        assertTrue(settings.getBoolean("enabled"));
        assertEquals(22, settings.getInt("port"));
        assertEquals("fallback", settings.getString("missing", "fallback"));
    }
}

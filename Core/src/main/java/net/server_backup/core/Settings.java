package net.server_backup.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable scalar/list snapshot safe to share with background operations. */
public final class Settings {
    private final Map<String, Object> values;
    public Settings(Map<String, Object> input) {
        Map<String, Object> copy = new HashMap<>();
        input.forEach((key, value) -> {
            if (value instanceof List<?>) copy.put(key, Collections.unmodifiableList(new ArrayList<>((List<?>) value)));
            else if (value instanceof String || value instanceof Number || value instanceof Boolean) copy.put(key, value);
        });
        values = Collections.unmodifiableMap(copy);
    }
    public Object get(String key) { return values.get(key); }
    public String getString(String key) { return getString(key, null); }
    public String getString(String key, String fallback) {
        Object value = values.get(key); return value instanceof String ? (String) value : fallback;
    }
    public int getInt(String key) { return getInt(key, 0); }
    public int getInt(String key, int fallback) {
        Object value = values.get(key); return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
    public boolean getBoolean(String key) { return Boolean.TRUE.equals(values.get(key)); }
    public List<String> getStringList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?>)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) if (item instanceof String) result.add((String) item);
        return Collections.unmodifiableList(result);
    }
}

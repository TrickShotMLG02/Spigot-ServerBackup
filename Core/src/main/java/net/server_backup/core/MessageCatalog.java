package net.server_backup.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Immutable formatted-message source suitable for worker threads. */
public final class MessageCatalog {
    private final String prefix;
    private final Map<String, String> messages;
    public MessageCatalog(String prefix, Map<String, String> messages) {
        this.prefix = prefix;
        this.messages = Collections.unmodifiableMap(new HashMap<>(messages));
    }
    public String format(String key) { return MessageFormatter.format(prefix, messages.get(key)); }
}

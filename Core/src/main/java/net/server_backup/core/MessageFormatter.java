package net.server_backup.core;

/** Formats configured messages without depending on a server implementation. */
public final class MessageFormatter {
    private MessageFormatter() {
    }

    public static String format(String prefix, String message) {
        return (prefix + message).replace("&nl", "\n").replace("&", "\u00a7");
    }
}

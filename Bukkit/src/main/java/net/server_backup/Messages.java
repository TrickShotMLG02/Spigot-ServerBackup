package net.server_backup;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;

public class Messages {

    private final Configuration configuration;
    private final YamlConfiguration messages;
    public Messages(Configuration configuration) { this.configuration = configuration; this.messages = configuration.messages; }

    public void loadMessages() {
        messages.options().header("Use '&nl' to add a new line. Use '&' for color codes (e.g. '&4' for color red). For some messages you can use a placeholder (e.g. '%file%' for file name)." +
                "\nMinecraft color codes: https://htmlcolorcodes.com/minecraft-color-codes/");
        messages.options().copyDefaults(true);

        net.server_backup.core.MessageDefaults.apply(messages::addDefault);

        configuration.saveMessages();

        configuration.prefix = ((messages.getString("Prefix").equals("")) ? messages.getString("Prefix") : (messages.getString("Prefix") + " "));
        java.util.Map<String, String> snapshot = new java.util.HashMap<>();
        net.server_backup.core.MessageDefaults.apply((key, ignored) -> snapshot.put(key, messages.getString(key)));
        for (String key : messages.getKeys(true)) {
            Object value = messages.get(key);
            if (value instanceof String) snapshot.put(key, (String) value);
        }
        configuration.messageCatalog = new net.server_backup.core.MessageCatalog(configuration.prefix, snapshot);
    }
}

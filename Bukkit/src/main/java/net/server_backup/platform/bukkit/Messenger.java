package net.server_backup.platform.bukkit;

import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/** Marshals command-sender messages onto the server thread. */
public final class Messenger {
    private Messenger() { }
    public static void send(ServerBackup plugin, CommandSender sender, String message) {
        if (Bukkit.isPrimaryThread()) sender.sendMessage(message);
        else {
            if (plugin.isEnabled()) plugin.platform().runOnServerThread(() -> sender.sendMessage(message));
        }
    }
}

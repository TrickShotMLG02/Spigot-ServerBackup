package net.server_backup.commands;

import net.server_backup.ServerBackup;
import net.server_backup.utils.DropboxManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandDropbox {

    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        if(args[1].equalsIgnoreCase("upload")) {
            plugin.platform().runAsync(() -> {
                DropboxManager dm = new DropboxManager(plugin, sender);

                dm.uploadToDropbox(args[2]);
            });
        }
    }
}

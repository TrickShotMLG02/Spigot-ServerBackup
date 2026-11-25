package net.server_backup.commands;

import net.server_backup.bukkit.Backup;
import org.bukkit.command.CommandSender;

public class CommandRemove {

    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        if (args[1].equalsIgnoreCase("Files")) {
            sender.sendMessage("You can not delete the 'Files' backup folder.");

            return;
        }

        Backup backup = new Backup(plugin, args[1], sender, true);

        backup.remove();
    }

}

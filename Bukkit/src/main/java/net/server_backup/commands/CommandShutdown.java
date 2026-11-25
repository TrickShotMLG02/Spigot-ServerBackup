package net.server_backup.commands;

import net.server_backup.bukkit.OperationHandler;
import org.bukkit.command.CommandSender;

public class CommandShutdown {

    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        if (plugin.operations().shutdown.toggle()) {
            sender.sendMessage(plugin.operations().processMessage("Command.Shutdown.Start"));
        } else sender.sendMessage(plugin.operations().processMessage("Command.Shutdown.Cancel"));
    }

}

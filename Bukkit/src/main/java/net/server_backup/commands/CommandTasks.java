package net.server_backup.commands;

import net.server_backup.bukkit.OperationHandler;
import org.bukkit.command.CommandSender;

public class CommandTasks {

    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        java.util.List<String> tasks = plugin.operations().tasks.snapshot();
        if (!tasks.isEmpty()) {
            sender.sendMessage(plugin.operations().processMessage("Command.Tasks.Header"));

            for (String task : tasks) {
                sender.sendMessage(task);
            }

            sender.sendMessage(plugin.operations().processMessage("Command.Tasks.Footer"));
        } else {
            sender.sendMessage(plugin.operations().processMessage("Error.NoTasks"));
        }
    }

}

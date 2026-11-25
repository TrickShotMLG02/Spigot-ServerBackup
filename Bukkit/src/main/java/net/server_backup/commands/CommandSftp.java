package net.server_backup.commands;

import org.bukkit.command.CommandSender;

public final class CommandSftp {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) { RemoteCommand.execute(plugin, sender, args, true); }
}

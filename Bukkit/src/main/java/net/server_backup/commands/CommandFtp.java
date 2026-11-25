package net.server_backup.commands;

import org.bukkit.command.CommandSender;

public final class CommandFtp {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) { RemoteCommand.execute(plugin, sender, args, false); }
}

package net.server_backup.commands;

import org.bukkit.command.CommandSender;

public final class CommandList {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) { CatalogCommand.execute(plugin, sender, null, args[1]); }
}

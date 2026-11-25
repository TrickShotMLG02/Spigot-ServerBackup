package net.server_backup.commands;

import org.bukkit.command.CommandSender;

public final class CommandSearch {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) { CatalogCommand.execute(plugin, sender, args[1], args[2]); }
}

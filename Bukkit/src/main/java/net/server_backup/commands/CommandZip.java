package net.server_backup.commands;

import org.bukkit.command.CommandSender;

public final class CommandZip {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) { ArchiveCommand.execute(plugin, sender, args[1], false); }
}

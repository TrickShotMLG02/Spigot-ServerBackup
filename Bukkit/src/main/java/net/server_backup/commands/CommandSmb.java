package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.utils.SmbManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

public final class CommandSmb {
    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        plugin.platform().runAsync(() -> {
            SmbManager manager = new SmbManager(plugin, sender);
            if (args[1].equalsIgnoreCase("list")) {
                List<String> backups = manager.getSmbBackupList();
                if (backups.isEmpty()) net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.NoSmbBackups"));
                else backups.forEach(message -> net.server_backup.platform.bukkit.Messenger.send(plugin, sender, message));
            } else if (args[1].equalsIgnoreCase("download")) {
                manager.downloadBackup(args[2]);
            } else if (args[1].equalsIgnoreCase("upload")) {
                File file = new File(plugin.configuration().backupDestination, args[2]);
                manager.uploadBackup(file.getPath());
            }
        });
    }
}

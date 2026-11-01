package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.OperationHandler;
import net.server_backup.utils.SmbManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

public final class CommandSmb {
    public static void execute(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(ServerBackup.getInstance(), () -> {
            SmbManager manager = new SmbManager(sender);
            if (args[1].equalsIgnoreCase("list")) {
                List<String> backups = manager.getSmbBackupList();
                if (backups.isEmpty()) sender.sendMessage(OperationHandler.processMessage("Error.NoSmbBackups"));
                else backups.forEach(sender::sendMessage);
            } else if (args[1].equalsIgnoreCase("download")) {
                manager.downloadBackup(args[2]);
            } else if (args[1].equalsIgnoreCase("upload")) {
                File file = new File(Configuration.backupDestination, args[2]);
                manager.uploadBackup(file.getPath());
            }
        });
    }
}

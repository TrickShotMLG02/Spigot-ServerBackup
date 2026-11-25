package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.Backup;
import net.server_backup.bukkit.OperationHandler;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class CommandCreate {

    public static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args) {
        net.server_backup.core.CreateRequest request = net.server_backup.core.CreateRequest.parse(args);
        File file = new File(request.source);

        if (request.isSingleFile()) {
            plugin.platform().runAsync(() -> {
                try {
                    new net.server_backup.core.LocalFileBackup().copy(file.toPath(),
                            java.nio.file.Paths.get(plugin.configuration().backupDestination), java.time.Clock.systemDefaultZone());
                    net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Info.BackupFinished").replace("%file%", args[1]));
                } catch (IOException e) {
                    net.server_backup.platform.bukkit.Messenger.send(plugin, sender, plugin.operations().processMessage("Error.BackupFailed").replace("%file%", args[1]));
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "File backup failed", e);
                }
            });
        } else {
            Backup backup = new Backup(plugin, request.source, sender, request.full);

            backup.create();
        }
    }

}

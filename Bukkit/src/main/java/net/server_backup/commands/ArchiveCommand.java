package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.core.ArchiveCommandPlanner;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.bukkit.ZipManager;
import org.bukkit.command.CommandSender;
import java.nio.file.Paths;
import java.io.IOException;

final class ArchiveCommand {
    static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String name, boolean unzip) {
        ArchiveCommandPlanner.Plan plan = new ArchiveCommandPlanner().plan(Paths.get(plugin.configuration().backupDestination), name, unzip);
        if (plan.error != null) {
            String code;
            switch (plan.error) {
                case ALREADY_ZIP: code = "Error.AlreadyZip"; break;
                case NOT_ZIP: code = "Error.NotAZip"; break;
                case NOT_FOUND: code = "Error.NoBackupFound"; break;
                case TARGET_EXISTS: code = unzip ? "Error.ZipExists" : "Error.FolderExists"; break;
                default: code = "Error.NoBackupFound";
            }
            sender.sendMessage(plugin.operations().processMessage(code).replace("%file%", name));
            return;
        }
        sender.sendMessage(plugin.operations().processMessage(unzip ? "Command.Unzip.Header" : "Command.Zip.Header"));
        ZipManager manager = new ZipManager(plugin, plan.source.toString(), plan.target.toString(), sender, !unzip, unzip, true);
        if (unzip) manager.unzip();
        else try { manager.zip(); } catch (IOException e) { sender.sendMessage(plugin.operations().processMessage("Error.BackupFailed").replace("%file%", name)); }
    }
}

package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.bukkit.OperationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Executor implements CommandExecutor {
    private final net.server_backup.ServerBackup plugin;
    public Executor(net.server_backup.ServerBackup plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        net.server_backup.core.CommandRouter.Route route = new net.server_backup.core.CommandRouter()
                .route(sender.hasPermission("backup.admin"), args);
        String[] forwarded = route.arguments;
        switch (route.action) {
            case DENIED: sender.sendMessage(plugin.operations().processMessage("Error.NoPermission")); break;
            case HELP: sendHelp(sender); break;
            case CREATE: CommandCreate.execute(plugin, sender, forwarded); break;
            case SHUTDOWN: CommandShutdown.execute(plugin, sender, forwarded); break;
            case RELOAD: plugin.configuration().reloadConfig(sender); break;
            case TASKS: CommandTasks.execute(plugin, sender, forwarded); break;
            case LIST: CommandList.execute(plugin, sender, forwarded); break;
            case SEARCH: CommandSearch.execute(plugin, sender, forwarded); break;
            case ZIP: CommandZip.execute(plugin, sender, forwarded); break;
            case UNZIP: CommandUnzip.execute(plugin, sender, forwarded); break;
            case REMOVE: CommandRemove.execute(plugin, sender, forwarded); break;
            case FTP: CommandFtp.execute(plugin, sender, forwarded); break;
            case SFTP: CommandSftp.execute(plugin, sender, forwarded); break;
            case SMB: CommandSmb.execute(plugin, sender, forwarded); break;
            case DROPBOX: CommandDropbox.execute(plugin, sender, forwarded); break;
            default: break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/backup reload - reloads the config");
        sender.sendMessage("");
        sender.sendMessage("/backup list <page> - shows a list of 10 backups");
        sender.sendMessage("");
        sender.sendMessage(
                "/backup search <search argument> <page> - shows a list of 10 backups that contain the given search argument");
        sender.sendMessage("");
        sender.sendMessage("/backup create <world> - creates a new backup of a world");
        sender.sendMessage("");
        sender.sendMessage("/backup remove <folder> - removes an existing backup");
        sender.sendMessage("");
        sender.sendMessage("/backup zip <folder> - zipping folder");
        sender.sendMessage("");
        sender.sendMessage("/backup unzip <file> - unzipping file");
        sender.sendMessage("");
        sender.sendMessage("/backup ftp <download/upload/list> - download, upload or list ftp backup files");
        sender.sendMessage("");
        sender.sendMessage("/backup sftp <download/upload/list> - download, upload or list sftp backup files");
        sender.sendMessage("");
        sender.sendMessage("/backup smb <download/upload/list> - download, upload or list SMB backup files");
        sender.sendMessage("");
        sender.sendMessage("/backup dropbox upload <file> - upload a backup to dropbox");
        sender.sendMessage("");
        sender.sendMessage("/backup shutdown - shut downs the server after backup tasks are finished");
    }

}

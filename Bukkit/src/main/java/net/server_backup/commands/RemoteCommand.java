package net.server_backup.commands;

import net.md_5.bungee.api.chat.*;
import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.core.Page;
import net.server_backup.utils.FtpManager;
import net.server_backup.utils.SftpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

final class RemoteCommand {
    static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String[] args, boolean sftp) {
        plugin.platform().runAsync(() -> {
            FtpManager ftp = sftp ? null : new FtpManager(plugin, sender);
            SftpManager secure = sftp ? new SftpManager(plugin, sender) : null;
            if (args[1].equalsIgnoreCase("upload")) {
                if (sftp) secure.uploadFileToSftp(args[2], false); else ftp.uploadFileToFtp(args[2], false);
            } else if (args[1].equalsIgnoreCase("download")) {
                if (sftp) secure.downloadFileFromSftp(args[2]); else ftp.downloadFileFromFtp(args[2]);
            } else if (args[1].equalsIgnoreCase("list")) {
                List<String> entries = sftp ? secure.getSftpBackupList(false) : ftp.getFtpBackupList(false);
                if (entries.isEmpty()) {
                    plugin.platform().runOnServerThread(() -> sender.sendMessage(plugin.operations().processMessage(sftp ? "Error.NoSftpBackups" : "Error.NoFtpBackups")));
                    return;
                }
                try {
                    Page<String> page = Page.of(entries, Integer.parseInt(args[2]));
                    plugin.platform().runOnServerThread(() -> render(sender, page, sftp));
                } catch (IllegalArgumentException e) {
                    plugin.platform().runOnServerThread(() -> sender.sendMessage(plugin.operations().processMessage("Error.NotANumber").replace("%input%", args[2])));
                }
            }
        });
    }
    private static void render(CommandSender sender, Page<String> page, boolean sftp) {
        int first = (page.number - 1) * 10 + 1;
        sender.sendMessage("----- " + (sftp ? "Sftp" : "Ftp") + "-Backup " + first + "-" + (first + page.items.size() - 1) + "/" + page.total + " -----");
        for (String entry : page.items) {
            if (sender instanceof Player) {
                TextComponent text = new TextComponent(entry);
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to get Backup name").create()));
                String[] pieces = entry.split(" ");
                text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, pieces.length > 1 ? pieces[1] : entry));
                ((Player) sender).spigot().sendMessage(text);
            } else sender.sendMessage(entry);
        }
        sender.sendMessage("--------- Page " + page.number + "/" + page.pages() + " ---------");
    }
}

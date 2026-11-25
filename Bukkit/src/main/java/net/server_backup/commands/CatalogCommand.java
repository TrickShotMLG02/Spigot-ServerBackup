package net.server_backup.commands;

import net.md_5.bungee.api.chat.*;
import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.BackupCatalog;
import net.server_backup.bukkit.OperationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.nio.file.Paths;

final class CatalogCommand {
    static void execute(net.server_backup.ServerBackup plugin, CommandSender sender, String query, String pageText) {
        final int page;
        try { page = Integer.parseInt(pageText); }
        catch (NumberFormatException e) { sender.sendMessage(plugin.operations().processMessage("Error.NotANumber").replace("%input%", pageText)); return; }
        plugin.platform().runAsync(() -> {
            try {
                BackupCatalog.Page result = new BackupCatalog().list(Paths.get(plugin.configuration().backupDestination), query, page);
                plugin.platform().runOnServerThread(() -> render(plugin, sender, query, result));
            } catch (IllegalArgumentException e) {
                plugin.platform().runOnServerThread(() -> sender.sendMessage("Try a lower positive page number."));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not list backups: " + e.getMessage());
                plugin.platform().runOnServerThread(() -> sender.sendMessage(plugin.operations().processMessage("Error.NoBackups")));
            }
        });
    }
    private static void render(net.server_backup.ServerBackup plugin, CommandSender sender, String query, BackupCatalog.Page page) {
        if (page.total == 0) {
            sender.sendMessage(plugin.operations().processMessage(query == null ? "Error.NoBackups" : "Error.NoBackupSearch")
                    .replace("%input%", query == null ? "" : query));
            return;
        }
        int first = (page.number - 1) * 10 + 1;
        sender.sendMessage("----- Backup " + first + "-" + (first + page.entries.size() - 1) + "/" + page.total + " -----");
        int index = first;
        for (BackupCatalog.Entry entry : page.entries) {
            if (sender instanceof Player) {
                double size = Math.round(entry.bytes / 1000000.0 * 100.0) / 100.0;
                TextComponent text = new TextComponent("\u00a77[" + index + "] \u00a7r" + entry.name + " \u00a77[" + size + "MB]");
                text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to get Backup name").create()));
                text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, query == null ? entry.name : "/backup remove " + entry.name));
                ((Player) sender).spigot().sendMessage(text);
            } else sender.sendMessage(entry.name);
            index++;
        }
        sender.sendMessage("-------- Page " + page.number + "/" + page.pages() + " --------");
    }
}

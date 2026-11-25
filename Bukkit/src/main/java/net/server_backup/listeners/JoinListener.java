package net.server_backup.listeners;

import net.server_backup.ServerBackup;
import net.server_backup.bukkit.OperationHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final ServerBackup plugin;
    public JoinListener(ServerBackup plugin) { this.plugin = plugin; }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("backup.update") && plugin.configuration().settings.getBoolean("UpdateAvailableMessage"))
            plugin.operations().checkVersion(event.getPlayer());
    }
}

package net.server_backup.bukkit;

import net.server_backup.core.*;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

/** Captures world locations on the server thread and records changed regions. */
public class DynamicBackup implements Listener {
    private final ServerBackup plugin;
    private BukkitTask pendingSave;
    public DynamicBackup(ServerBackup plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        if (from.getWorld() != to.getWorld() || (from.getBlockX() >> 4) != (to.getBlockX() >> 4)
                || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4)) record(to);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { record(event.getPlayer().getLocation()); }

    private void record(Location location) {
        if (!plugin.configuration().settings.getBoolean("DynamicBackup") || location.getWorld() == null) return;
        String key = RegionKey.forChunk(Bukkit.getWorldContainer().toPath(), location.getWorld().getName(),
                location.getBlockX() >> 4, location.getBlockZ() >> 4);
        synchronized (plugin.configuration().backupInfo) {
            if (plugin.configuration().backupInfo.contains(key)) return;
            plugin.configuration().backupInfo.set(key, true);
        }
        if (pendingSave == null) {
            pendingSave = Bukkit.getScheduler().runTaskLater(plugin, this::flush, 100L);
        }
    }

    public void flush() {
        if (pendingSave != null) { pendingSave.cancel(); pendingSave = null; }
        plugin.configuration().saveBackupInfo();
    }
}

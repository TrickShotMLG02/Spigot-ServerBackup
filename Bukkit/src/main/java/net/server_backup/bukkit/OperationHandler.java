package net.server_backup.bukkit;

import net.server_backup.core.*;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Scanner;
import java.util.logging.Level;

public class OperationHandler {
    private final ServerBackup plugin;
    public OperationHandler(ServerBackup plugin) { this.plugin = plugin; }

    public final ShutdownRequest shutdown = new ShutdownRequest();
    public volatile boolean isUpdated = false;
    private final Object UPDATE_LOCK = new Object();

    public final OperationRegistry tasks = new OperationRegistry();
    public final java.util.concurrent.atomic.AtomicBoolean postCommandScheduled = new java.util.concurrent.atomic.AtomicBoolean();
    private BukkitTask timer;

    public String processMessage(String msgCode) {
        return plugin.configuration().messageCatalog.format(msgCode);
    }

    public void startTimer() {
        stopTimer();
        timer = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Timer(plugin), 20 * 20, 20 * 20);
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void checkVersion() { checkVersion(null); }

    public void checkVersion(org.bukkit.command.CommandSender recipient) {
        String current = plugin.getDescription().getVersion();
        boolean automatic = plugin.configuration().settings.getBoolean("AutomaticUpdates");
        String serverVersion = Bukkit.getVersion();
        java.nio.file.Path destination = Bukkit.getUpdateFolderFile().toPath().resolve(plugin.pluginJarName());
        plugin.platform().runAsync(() -> {
            try {
                UpdateService service = new UpdateService();
                String latest = service.latest(new URL("https://api.spigotmc.org/legacy/update.php?resource=79320"));
                if (!service.isNewer(latest, current)) return;
                plugin.getLogger().info("ServerBackup update available: " + latest + " (current " + current + ")");
                if (automatic) synchronized (UPDATE_LOCK) {
                    if (!isUpdated) {
                        String path = serverVersion.contains("1.18") || serverVersion.contains("1.19")
                                ? "https://server-backup.net/assets/downloads/ServerBackup.jar"
                                : "https://server-backup.net/assets/downloads/alt/ServerBackup.jar";
                        service.download(new URL(path), destination);
                        isUpdated = true;
                    }
                }
                if (recipient != null) plugin.platform().runOnServerThread(() -> recipient.sendMessage(
                        "ServerBackup: Version " + latest + " is available. " +
                                (isUpdated ? "Update staged for the next restart." : "Download it from https://server-backup.net/")));
            } catch (IOException | IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "ServerBackup update check failed", e);
            }
        });
    }
}

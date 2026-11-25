package net.server_backup.bootstrap;

import net.server_backup.ServerBackup;
import net.server_backup.platform.bukkit.BukkitPlatform;
import net.server_backup.platform.paper.PaperPlatform;

/** Bukkit-compatible entry point for the shared distribution. */
public final class ServerBackupBootstrap extends ServerBackup {
    @Override public void onEnable() {
        boolean paper = PlatformSelection.isPaper(getServer().getName(), this::classAvailable);
        setPlatform(paper ? new PaperPlatform(this) : new BukkitPlatform(this));
        getLogger().info("Selected platform adapter: " + platform().name());
        super.onEnable();
    }
    private boolean classAvailable(String name) {
        try { Class.forName(name, false, getClassLoader()); return true; }
        catch (ClassNotFoundException | LinkageError e) { return false; }
    }
}

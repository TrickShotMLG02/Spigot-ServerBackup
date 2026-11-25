package net.server_backup.platform.paper;

import net.server_backup.platform.bukkit.BukkitPlatform;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper uses the common Bukkit APIs for the currently required operations. */
public final class PaperPlatform extends BukkitPlatform {
    public PaperPlatform(JavaPlugin plugin) { super(plugin); }
    @Override public String name() { return "Paper"; }
}

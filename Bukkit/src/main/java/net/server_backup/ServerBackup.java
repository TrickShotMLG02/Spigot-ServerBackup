package net.server_backup;

import net.server_backup.commands.Executor;
import net.server_backup.commands.TabCompleter;
import net.server_backup.bukkit.DynamicBackup;
import net.server_backup.bukkit.OperationHandler;
import net.server_backup.listeners.JoinListener;
import net.server_backup.utils.BStats;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ServerBackup extends JavaPlugin {

    private final OperationHandler operations = new OperationHandler(this);
    public final OperationHandler operations() { return operations; }
    private Configuration configuration;
    public final Configuration configuration() { return configuration; }
    private DynamicBackup dynamicBackup;
    private net.server_backup.core.platform.PlatformRuntime platform;
    protected final void setPlatform(net.server_backup.core.platform.PlatformRuntime platform) { this.platform = platform; }
    public final net.server_backup.core.platform.PlatformRuntime platform() { return platform; }
    public final String pluginJarName() { return getFile().getName(); }

    @Override
    public void onDisable() {
        if (dynamicBackup != null) dynamicBackup.flush();
        operations.stopTimer();
        if (platform != null) {
            platform.cancelTasks();
            if (!platform.shutdown()) getLogger().warning("Backup operations did not finish within 20 seconds: " + operations.tasks.snapshot());
        }

        Bukkit.getLogger().log(Level.INFO, "ServerBackup: Plugin disabled.");
    }

    @Override
    public void onEnable() {
        configuration = new Configuration(this);
        configuration.loadUp();

        operations.startTimer();

        getCommand("backup").setExecutor(new Executor(this));
        getCommand("backup").setTabCompleter(new TabCompleter(this));

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        dynamicBackup = new DynamicBackup(this);
        Bukkit.getPluginManager().registerEvents(dynamicBackup, this);

        Bukkit.getLogger().log(Level.INFO, "ServerBackup: Plugin enabled.");

        if (configuration.settings.getBoolean("UpdateAvailableMessage")) {
            operations.checkVersion();
        }

        BStats.initialize(this);
    }

}

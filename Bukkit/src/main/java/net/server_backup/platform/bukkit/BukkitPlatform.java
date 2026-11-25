package net.server_backup.platform.bukkit;

import net.server_backup.core.platform.PlatformRuntime;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BukkitPlatform implements PlatformRuntime {
    protected final JavaPlugin plugin;
    private final ExecutorService workers;
    private final AtomicInteger outstanding = new AtomicInteger();
    public BukkitPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.workers = Executors.newFixedThreadPool(4, task -> {
            Thread thread = new Thread(task, "ServerBackup-worker");
            thread.setDaemon(true);
            return thread;
        });
    }
    public String name() { return "Bukkit/Spigot"; }
    public void runOnServerThread(Runnable task) {
        if (!plugin.isEnabled()) return;
        try { Bukkit.getScheduler().runTask(plugin, task); }
        catch (org.bukkit.plugin.IllegalPluginAccessException e) {
            if (plugin.isEnabled()) throw e;
        }
    }
    public void runAsync(Runnable task) {
        outstanding.incrementAndGet();
        try {
            workers.execute(() -> {
                try { task.run(); } finally { outstanding.decrementAndGet(); }
            });
        } catch (RuntimeException e) { outstanding.decrementAndGet(); throw e; }
    }
    public boolean hasActiveWork() { return outstanding.get() > 0; }
    public void cancelTasks() { Bukkit.getScheduler().cancelTasks(plugin); }
    public boolean shutdown() {
        workers.shutdown();
        try {
            if (workers.awaitTermination(20, TimeUnit.SECONDS)) return true;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        outstanding.addAndGet(-workers.shutdownNow().size());
        return false;
    }
}

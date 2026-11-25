package net.server_backup.core.platform;

/** Server-thread dispatch provided by a runtime adapter. */
public interface PlatformRuntime {
    String name();
    void runOnServerThread(Runnable task);
    void runAsync(Runnable task);
    boolean hasActiveWork();
    void cancelTasks();
    /** Stop accepting work and wait briefly for active workers; return false on timeout. */
    boolean shutdown();
}

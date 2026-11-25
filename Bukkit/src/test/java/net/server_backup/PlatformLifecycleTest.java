package net.server_backup;

import net.server_backup.platform.bukkit.BukkitPlatform;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class PlatformLifecycleTest {
    @Test void shutdownWaitsForWorkersAndRejectsNewWork() throws Exception {
        BukkitPlatform platform = new BukkitPlatform(null);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), completed = new CountDownLatch(1);
        platform.runAsync(() -> {
            started.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            completed.countDown();
        });
        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertTrue(platform.hasActiveWork());
        release.countDown();
        assertTrue(platform.shutdown());
        assertTrue(completed.await(1, TimeUnit.SECONDS));
        assertFalse(platform.hasActiveWork());
        assertThrows(RejectedExecutionException.class, () -> platform.runAsync(() -> { }));
    }
}

package net.server_backup.core;

import java.util.concurrent.atomic.AtomicBoolean;

/** Thread-safe state for the deferred shutdown command. */
public final class ShutdownRequest {
    private final AtomicBoolean requested = new AtomicBoolean();
    public boolean isRequested() { return requested.get(); }
    public boolean toggle() {
        boolean previous;
        do { previous = requested.get(); } while (!requested.compareAndSet(previous, !previous));
        return !previous;
    }
}

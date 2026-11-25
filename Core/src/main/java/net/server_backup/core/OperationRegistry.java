package net.server_backup.core;

import java.util.ArrayList;
import java.util.List;

/** Tracks ongoing operations and exposes consistent snapshots to callers. */
public final class OperationRegistry {
    private final List<String> active = new ArrayList<>();
    public synchronized void add(String task) { active.add(task); }
    public synchronized boolean remove(String task) { return active.remove(task); }
    public synchronized void replace(String previous, String next) {
        int index = active.indexOf(previous);
        if (index >= 0) active.set(index, next);
    }
    public synchronized boolean isEmpty() { return active.isEmpty(); }
    public synchronized int size() { return active.size(); }
    public synchronized List<String> snapshot() { return new ArrayList<>(active); }
}

package net.server_backup.core;

import java.util.List;

/** Immutable ten-entry command page. */
public final class Page<T> {
    public final int number, total;
    public final List<T> items;
    private Page(int number, int total, List<T> items) { this.number = number; this.total = total; this.items = items; }
    public int pages() { return (total + 9) / 10; }
    public static <T> Page<T> of(List<T> items, int number) {
        long offset = ((long) number - 1) * 10;
        if (number < 1 || (offset >= items.size() && !items.isEmpty())) throw new IllegalArgumentException("Invalid page");
        int start = (int) Math.min(offset, items.size());
        return new Page<>(number, items.size(), List.copyOf(items.subList(start, Math.min(start + 10, items.size()))));
    }
}

package net.server_backup.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reads and paginates local backup entries. */
public final class BackupCatalog {
    public static final class Entry {
        public final String name;
        public final long bytes;
        public Entry(String name, long bytes) { this.name = name; this.bytes = bytes; }
    }
    public static final class Page {
        public final List<Entry> entries;
        public final int number, total;
        public Page(List<Entry> entries, int number, int total) { this.entries = entries; this.number = number; this.total = total; }
        public int pages() { return (total + 9) / 10; }
    }
    public Page list(Path directory, String query, int page) throws IOException {
        if (page < 1) throw new IllegalArgumentException("Page must be positive");
        List<Path> files;
        try (Stream<Path> paths = Files.list(directory)) {
            files = paths.filter(p -> query == null ? !p.getFileName().toString().equalsIgnoreCase("Files")
                            : p.getFileName().toString().contains(query))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
        }
        long offset = (long) (page - 1) * 10;
        if (offset >= files.size() && !files.isEmpty()) throw new IllegalArgumentException("Page out of range");
        int start = (int) Math.min(offset, files.size());
        List<Entry> entries = new java.util.ArrayList<>();
        for (Path file : files.subList(start, Math.min(start + 10, files.size()))) {
            long size;
            try (Stream<Path> paths = Files.walk(file)) {
                size = paths.filter(Files::isRegularFile).mapToLong(p -> {
                    try { return Files.size(p); } catch (IOException e) { return 0; }
                }).sum();
            }
            entries.add(new Entry(file.getFileName().toString(), size));
        }
        return new Page(entries, page, files.size());
    }
}

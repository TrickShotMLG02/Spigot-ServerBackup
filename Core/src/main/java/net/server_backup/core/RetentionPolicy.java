package net.server_backup.core;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects entries using the legacy count and date retention rules. */
public final class RetentionPolicy {
    private static final java.util.regex.Pattern BACKUP_NAME = java.util.regex.Pattern.compile(
            "^(?:dynamic-)?backup-(\\d{4}-\\d{2}-\\d{2})~\\d{2}-\\d{2}-\\d{2}-(.+?)(?:\\.zip)?$");
    public List<File> select(File[] entries, int limit, int maxAgeDays, boolean keepUnique, LocalDate today) {
        if (entries == null) throw new IllegalArgumentException("Cannot read backup directory");
        // The Files directory stores individually backed-up files and is not an archive.
        File[] sorted = Arrays.stream(entries)
                .filter(entry -> !entry.getName().equalsIgnoreCase("Files"))
                .toArray(File[]::new);
        List<File> selected = new ArrayList<>();
        if (limit > 0) {
            sorted = Arrays.stream(sorted).filter(entry -> BACKUP_NAME.matcher(entry.getName()).matches()).toArray(File[]::new);
            Arrays.sort(sorted, java.util.Comparator.comparing(entry -> entry.getName().replaceFirst("^dynamic-", "")));
            selected.addAll(Arrays.asList(sorted).subList(0, Math.max(0, sorted.length - limit)));
            return selected;
        }
        if (maxAgeDays <= 0) return selected;
        Arrays.sort(sorted, java.util.Comparator.comparing(
                (File entry) -> entry.getName().replaceFirst("^dynamic-", "")).reversed());
        Set<String> names = new HashSet<>();
        LocalDate cutoff = today.minusDays(maxAgeDays);
        for (File entry : sorted) {
            try {
                java.util.regex.Matcher matcher = BACKUP_NAME.matcher(entry.getName());
                if (!matcher.matches()) continue;
                LocalDate date = LocalDate.parse(matcher.group(1));
                String name = matcher.group(2);
                if (keepUnique && names.add(name)) continue;
                if (!date.isAfter(cutoff)) selected.add(entry);
            } catch (RuntimeException ignored) {
                // Ignore files that do not use the existing backup naming convention.
            }
        }
        return selected;
    }
}

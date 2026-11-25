package net.server_backup.core;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;

/** Evaluates the existing weekday and hh-mm schedule configuration. */
public final class BackupSchedule {
    public boolean matches(DayOfWeek day, LocalTime now, Collection<String> days, String time) {
        boolean enabled = days.stream().anyMatch(value -> value.equalsIgnoreCase(day.name()));
        if (!enabled) return false;
        String[] parts = time.split("-", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid backup time: " + time);
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            LocalTime configured = LocalTime.of(hour, minute);
            return configured.getHour() == now.getHour() && configured.getMinute() == now.getMinute();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid backup time: " + time, e);
        }
    }
}

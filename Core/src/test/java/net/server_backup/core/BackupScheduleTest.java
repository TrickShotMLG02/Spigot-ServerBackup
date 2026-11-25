package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BackupScheduleTest {
    private final BackupSchedule schedule = new BackupSchedule();

    @Test void matchesConfiguredDayAndMinute() {
        assertTrue(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(0, 0, 30), List.of("monday"), "00-00"));
        assertFalse(schedule.matches(DayOfWeek.TUESDAY, LocalTime.MIDNIGHT, List.of("monday"), "00-00"));
        assertFalse(schedule.matches(DayOfWeek.MONDAY, LocalTime.of(0, 1), List.of("monday"), "00-00"));
    }

    @Test void rejectsInvalidTimeOnEnabledDay() {
        assertThrows(IllegalArgumentException.class, () -> schedule.matches(DayOfWeek.MONDAY, LocalTime.NOON, List.of("MONDAY"), "24-00"));
    }
}

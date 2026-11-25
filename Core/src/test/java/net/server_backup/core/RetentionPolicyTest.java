package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RetentionPolicyTest {
    private final RetentionPolicy policy = new RetentionPolicy();

    @Test void countKeepsNewestAccordingToLegacyNameOrdering() {
        File oldest = new File("dynamic-backup-2025-01-01~00-00-00-world.zip");
        File newest = new File("backup-2025-01-02~00-00-00-world.zip");
        File unrelated = new File("important.txt");
        File[] files = {newest, unrelated, oldest};
        assertEquals(List.of(oldest), policy.select(files, 1, 10, false, LocalDate.now()));
        assertEquals(newest, files[0]);
    }

    @Test void ageKeepsNewestForEachNameAndIgnoresUnrelatedFiles() {
        File old = new File("backup-2025-01-01~00-00-00-world.zip");
        File recent = new File("backup-2025-01-02~00-00-00-world.zip");
        File invalid = new File("Files");
        assertEquals(List.of(old), policy.select(new File[]{old, recent, invalid}, 0, 1, true,
                LocalDate.of(2025, 1, 10)));
    }

    @Test void disabledAgePolicyDoesNotRemoveFiles() {
        assertTrue(policy.select(new File[]{new File("backup-2020-01-01~00-00-00-world.zip")},
                0, 0, false, LocalDate.now()).isEmpty());
    }

    @Test void countLimitDoesNotSelectTheIndividualFilesDirectory() {
        File files = new File("Files");
        File oldest = new File("backup-2025-01-01~00-00-00-world.zip");
        File newest = new File("backup-2025-01-02~00-00-00-world.zip");
        assertEquals(List.of(oldest), policy.select(new File[]{files, oldest, newest}, 1, 0, false, LocalDate.now()));
    }

    @Test void uniqueRetentionUsesFullWorldNameRatherThanTimestampSeconds() {
        File first = new File("backup-2025-01-01~00-00-00-world-one.zip");
        File second = new File("backup-2025-01-01~00-00-00-world-two.zip");
        assertTrue(policy.select(new File[]{first, second}, 0, 1, true, LocalDate.of(2025, 1, 10)).isEmpty());
    }

    @Test void keepsNewestAcrossFullAndDynamicBackups() {
        File olderDynamic = new File("dynamic-backup-2025-01-01~00-00-00-world.zip");
        File newerFull = new File("backup-2025-01-02~00-00-00-world.zip");
        assertEquals(List.of(olderDynamic), policy.select(new File[]{olderDynamic, newerFull}, 0, 1, true, LocalDate.of(2025, 1, 10)));
    }
}

package net.server_backup.core;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Validates the existing configuration keys without depending on a YAML implementation. */
public final class ConfigurationValidation {
    public List<String> validate(Function<String, Object> value) {
        List<String> issues = new ArrayList<>();
        Object destination = value.apply("BackupDestination");
        if (!(destination instanceof String) || ((String) destination).isBlank()) issues.add("BackupDestination must not be empty");
        if (Boolean.TRUE.equals(value.apply("AutomaticBackups"))) {
            Object times = value.apply("BackupTimer.Times");
            if (!(times instanceof List) || ((List<?>) times).isEmpty()) issues.add("BackupTimer.Times must contain at least one time");
            else for (Object time : (List<?>) times) {
                try {
                    if (!(time instanceof String)) throw new IllegalArgumentException();
                    new BackupSchedule().matches(DayOfWeek.MONDAY, LocalTime.MIDNIGHT, List.of("MONDAY"), (String) time);
                } catch (IllegalArgumentException e) { issues.add("Invalid BackupTimer.Times entry: " + time); }
            }
        }
        checkPort(value, issues, "Ftp.UploadBackup", "Ftp.Server.Port");
        checkPort(value, issues, "Sftp.UploadBackup", "Sftp.Server.Port");
        checkPort(value, issues, "Smb.UploadBackup", "Smb.Server.Port");
        if (Boolean.TRUE.equals(value.apply("Sftp.UploadBackup"))) {
            Object fingerprint = value.apply("Sftp.Server.Fingerprint");
            if (!(fingerprint instanceof String) || ((String) fingerprint).isBlank() || ((String) fingerprint).contains("xxxxxxxx"))
                issues.add("Sftp.Server.Fingerprint must identify the server host key");
        }
        return issues;
    }
    private void checkPort(Function<String, Object> value, List<String> issues, String enabledKey, String portKey) {
        if (!Boolean.TRUE.equals(value.apply(enabledKey))) return;
        Object port = value.apply(portKey);
        if (!(port instanceof Number) || ((Number) port).intValue() < 1 || ((Number) port).intValue() > 65535)
            issues.add(portKey + " must be between 1 and 65535");
    }
}

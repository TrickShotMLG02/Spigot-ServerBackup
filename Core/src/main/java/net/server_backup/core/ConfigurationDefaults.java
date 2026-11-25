package net.server_backup.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Default settings used by the existing configuration format. */
public final class ConfigurationDefaults {
    private ConfigurationDefaults() { }
    public static void apply(BiConsumer<String, Object> defaults) {
        defaults.accept("AutomaticBackups", true);
        defaults.accept("CommandAfterAutomaticBackup", "/");

        List<String> days = new ArrayList<>();
        days.add("MONDAY");
        days.add("TUESDAY");
        days.add("WEDNESDAY");
        days.add("THURSDAY");
        days.add("FRIDAY");
        days.add("SATURDAY");
        days.add("SUNDAY");

        List<String> times = new ArrayList<>();
        times.add("00-00");

        defaults.accept("BackupTimer.Days", days);
        defaults.accept("BackupTimer.Times", times);

        List<String> worlds = new ArrayList<>();
        worlds.add("world");
        worlds.add("world_nether");
        worlds.add("world_the_end");

        defaults.accept("BackupWorlds", worlds);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("libraries");
        blacklist.add("plugins/ServerBackup/config.yml");

        defaults.accept("Blacklist", blacklist);

        defaults.accept("DeleteOldBackups", 14);
        defaults.accept("BackupLimiter", 0);

        defaults.accept("KeepUniqueBackups", false);

        defaults.accept("UpdateAvailableMessage", true);
        defaults.accept("AutomaticUpdates", true);

        defaults.accept("BackupDestination", "Backups//");

        defaults.accept("CloudBackup.Dropbox", false);
        defaults.accept("CloudBackup.Options.Destination", "/");
        defaults.accept("CloudBackup.Options.DeleteLocalBackup", false);

        defaults.accept("Ftp.UploadBackup", false);
        defaults.accept("Ftp.DeleteLocalBackup", false);
        defaults.accept("Ftp.Server.IP", "127.0.0.1");
        defaults.accept("Ftp.Server.Port", 21);
        defaults.accept("Ftp.Server.User", "username");
        defaults.accept("Ftp.Server.Password", "password");
        defaults.accept("Ftp.Server.BackupDirectory", "Backups/");

        defaults.accept("Sftp.UploadBackup", false);
        defaults.accept("Sftp.DeleteLocalBackup", false);
        defaults.accept("Sftp.Server.IP", "127.0.0.1");
        defaults.accept("Sftp.Server.Port", 22);
        defaults.accept("Sftp.Server.User", "username");
        defaults.accept("Sftp.Server.Password", "password");
        defaults.accept("Sftp.Server.Fingerprint", "SHA256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        defaults.accept("Sftp.Server.BackupDirectory", "Backups/");

        defaults.accept("Smb.UploadBackup", false);
        defaults.accept("Smb.DeleteLocalBackup", false);
        defaults.accept("Smb.Server.Host", "127.0.0.1");
        defaults.accept("Smb.Server.Port", 445);
        defaults.accept("Smb.Server.Share", "Backups");
        defaults.accept("Smb.Server.Domain", "");
        defaults.accept("Smb.Server.User", "username");
        defaults.accept("Smb.Server.Password", "password");
        defaults.accept("Smb.Server.Directory", "");

        defaults.accept("DynamicBackup", false);
        defaults.accept("SendLogMessages", false);

    }
}

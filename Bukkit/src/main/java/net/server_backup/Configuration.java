package net.server_backup;

import net.server_backup.bukkit.OperationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Configuration {

    private final ServerBackup plugin;
    public String prefix;
    public volatile net.server_backup.core.MessageCatalog messageCatalog;
    public volatile String backupDestination = "Backups//";
    public volatile net.server_backup.core.Settings settings = new net.server_backup.core.Settings(java.util.Collections.emptyMap());
    private final File bpInf, cloudInf, messagesFile;
    public final YamlConfiguration backupInfo, cloudInfo, messages;

    public Configuration(ServerBackup plugin) {
        this.plugin = plugin;
        File data = plugin.getDataFolder();
        bpInf = new File(data, "backupInfo.yml");
        cloudInf = new File(data, "cloudAccess.yml");
        messagesFile = new File(data, "messages.yml");
        backupInfo = YamlConfiguration.loadConfiguration(bpInf);
        cloudInfo = YamlConfiguration.loadConfiguration(cloudInf);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void loadUp() {
        loadConfig();
        loadCloud();
        loadBackupInfo();
        loadMessages();
    }

    public void loadConfig() {
        if (plugin.getConfig().contains("BackupDestination")) {
            backupDestination = plugin.getConfig().getString("BackupDestination");
        }

        if (backupDestination == null || backupDestination.isBlank()) {
            plugin.getLogger().warning("BackupDestination is empty; using Backups//");
            backupDestination = "Backups//";
            plugin.getConfig().set("BackupDestination", backupDestination);
        }

        if (!Files.exists(Paths.get(backupDestination))) {
            try {
                Files.createDirectories(Paths.get(backupDestination));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File files = new File(backupDestination + "//Files");

        if (!files.exists()) {
            files.mkdir();
        }

        plugin.getConfig().options()
                .header("BackupTimer = At what time should a Backup be created? The format is: 'hh-mm' e.g. '12-30'."
                        + "\nDeleteOldBackups = Deletes old backups automatically after a specific time (in days, standard = 7 days)"
                        + "\nDeleteOldBackups - Type '0' at DeleteOldBackups to disable the deletion of old backups."
                        + "\nBackupLimiter = Deletes old backups automatically if number of total backups is greater than this number (e.g. if you enter '5' - the oldest backup will be deleted if there are more than 5 backups, so you will always keep the latest 5 backups)"
                        + "\nBackupLimiter - Type '0' to disable this feature. If you don't type '0' the feature 'DeleteOldBackups' will be disabled and this feature ('BackupLimiter') will be enabled."
                        + "\nKeepUniqueBackups - Type 'true' to disable the deletion of unique backups. The plugin will keep the newest backup of all backed up worlds or folders, no matter how old it is."
                        + "\nBlacklist - A list of files/directories that will not be backed up."
                        + "\nSet 'UploadBackup' to 'true' if you want to store your backups on a ftp/sftp server."
                        + "\nIf you use ftp/sftp backups, you can set 'DeleteLocalBackup' to 'true' if you want the plugin to remove the created backup from your server once it has been uploaded to your ftp/sftp server."
                        + "\nCompressBeforeUpload compresses the backup to a zip file before uploading it. Set it to 'false' if you want the files to be uploaded directly to your ftp server."
                        + "\nJoin the discord server if you need help or have a question: https://discord.gg/rNzngsCWFC");
        plugin.getConfig().options().copyDefaults(true);

        net.server_backup.core.ConfigurationDefaults.apply(plugin.getConfig()::addDefault);
        for (String issue : new net.server_backup.core.ConfigurationValidation().validate(plugin.getConfig()::get))
            plugin.getLogger().warning("Configuration: " + issue);

        plugin.saveConfig();

        backupDestination = plugin.getConfig().getString("BackupDestination");
        java.util.Map<String, Object> values = new java.util.HashMap<>(plugin.getConfig().getValues(true));
        net.server_backup.core.ConfigurationDefaults.apply((key, ignored) -> values.put(key, plugin.getConfig().get(key)));
        settings = new net.server_backup.core.Settings(values);
    }

    public void loadCloud() {
        if (!cloudInf.exists()) {
            try {
                cloudInf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        cloudInfo.options().header("Dropbox - Watch this video for explanation: https://youtu.be/k-0aIohxRUA");

        new net.server_backup.core.DropboxCredentials().initialize(new net.server_backup.core.DropboxCredentials.Store() {
            public boolean contains(String key) { return cloudInfo.contains(key); }
            public String get(String key) { return cloudInfo.getString(key); }
            public void set(String key, String value) { cloudInfo.set(key, value); }
        });

        saveCloud();
    }

    public void saveCloud() {
        synchronized (cloudInfo) {
            try {
                cloudInfo.save(cloudInf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadBackupInfo() {
        if (!bpInf.exists()) {
            try {
                bpInf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveBackupInfo();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        new Messages(this).loadMessages();
    }

    public void saveBackupInfo() {
        synchronized (backupInfo) {
            try {
                backupInfo.save(bpInf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig(CommandSender sender) {
        plugin.reloadConfig();

        plugin.operations().stopTimer();

        String oldDes = backupDestination;

        if (!oldDes
                .equalsIgnoreCase(plugin.getConfig().getString("BackupDestination"))) {
            backupDestination = plugin.getConfig()
                    .getString("BackupDestination");

            plugin.getLogger().log(Level.INFO,
                    "ServerBackup: Backup destination [" + oldDes + " >> "
                            + backupDestination + "] updated successfully.");
        }

        if (cloudInf.exists()) {
            saveCloud();
        }

        loadUp();
        plugin.operations().startTimer();

        sender.sendMessage(plugin.operations().processMessage("Command.Reload"));
    }

}

package net.server_backup.core;

import java.util.function.BiConsumer;

/** Default localization entries shared by platform adapters. */
public final class MessageDefaults {
    private MessageDefaults() { }
    public static void apply(BiConsumer<String, Object> defaults) {
        defaults.accept("Prefix", "");

        defaults.accept("Command.Zip.Header", "Zipping Backup..."
                + "&nl");
        defaults.accept("Command.Zip.Footer", "&nl"
                + "&nlBackup [%file%] zipped."
                + "&nlBackup [%file%] saved.");
        defaults.accept("Command.Unzip.Header", "Unzipping Backup..."
                + "&nl");
        defaults.accept("Command.Unzip.Footer", "&nl"
                + "&nlBackup [%file%] unzipped.");
        defaults.accept("Command.Reload", "Config reloaded.");
        defaults.accept("Command.Tasks.Header", "----- Backup tasks -----"
                + "&nl");
        defaults.accept("Command.Tasks.Footer", "&nl"
                + "----- Backup tasks -----");
        defaults.accept("Command.Shutdown.Start", "The server will shut down after backup tasks (check with: '/backup tasks') are finished."
                + "&nlYou can cancel the shutdown by running this command again.");
        defaults.accept("Command.Shutdown.Cancel", "Shutdown canceled.");

        defaults.accept("Info.BackupFinished", "Backup [%file%] saved.");
        defaults.accept("Info.BackupStarted", "Backup [%file%] started.");
        defaults.accept("Info.BackupRemoved", "Backup [%file%] removed.");
        defaults.accept("Info.FtpUpload", "Ftp: Uploading backup [%file%] ...");
        defaults.accept("Info.FtpUploadSuccess", "Ftp: Upload successfully. Backup stored on ftp server.");
        defaults.accept("Info.FtpDownload", "Ftp: Downloading backup [%file%] ...");
        defaults.accept("Info.FtpDownloadSuccess", "Ftp: Download successful. Backup downloaded from ftp server.");
        defaults.accept("Info.SftpUpload", "Sftp: Uploading backup [%file%] ...");
        defaults.accept("Info.SftpUploadSuccess", "Sftp: Upload successfully. Backup stored on sftp server.");
        defaults.accept("Info.SmbUpload", "SMB: Uploading backup [%file%] ...");
        defaults.accept("Info.SmbUploadSuccess", "SMB: Backup uploaded successfully.");
        defaults.accept("Info.SmbDownloadSuccess", "SMB: Backup downloaded successfully.");
        defaults.accept("Info.SftpDownload", "Sftp: Downloading backup [%file%] ...");
        defaults.accept("Info.SftpDownloadSuccess", "Sftp: Download successful. Backup downloaded from sftp server.");

        defaults.accept("Error.NoPermission", "&cI'm sorry but you do not have permission to perform this command.");
        defaults.accept("Error.NoBackups", "No backups found.");
        defaults.accept("Error.NoBackupFound", "No Backup named '%file%' found.");
        defaults.accept("Error.NoBackupSearch", "No backups for search argument '%input%' found.");
        defaults.accept("Error.DeletionFailed", "Error while deleting '%file%'.");
        defaults.accept("Error.FolderExists", "There is already a folder named '%file%'.");
        defaults.accept("Error.ZipExists", "There is already a ZIP file named '%file%.zip'.");
        defaults.accept("Error.NoFtpBackups", "No ftp backups found.");
        defaults.accept("Error.NoSftpBackups", "No sftp backups found.");
        defaults.accept("Error.NoTasks", "No backup tasks are running.");
        defaults.accept("Error.AlreadyZip", "%file% is already a ZIP file.");
        defaults.accept("Error.NotAZip", "%file% is not a ZIP file.");
        defaults.accept("Error.NotANumber", "%input% is not a valid number.");
        defaults.accept("Error.BackupFailed", "An error occurred while saving Backup [%file%]. See console for more information.");
        defaults.accept("Error.FtpUploadFailed", "Ftp: Error while uploading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
        defaults.accept("Error.FtpDownloadFailed", "Ftp: Error while downloading backup to ftp server. Check server details in config.yml (ip, port, user, password).");
        defaults.accept("Error.FtpLocalDeletionFailed", "Ftp: Local backup deletion failed because the uploaded file was not found on the ftp server. Try again.");
        defaults.accept("Error.FtpNotFound", "Ftp: ftp-backup %file% not found.");
        defaults.accept("Error.FtpConnectionFailed", "Ftp: Error while connecting to FTP server.");
        defaults.accept("Error.SftpUploadFailed", "Sftp: Error while uploading backup to sftp server. Check server details in config.yml (ip, port, user, password).");
        defaults.accept("Error.SftpDownloadFailed", "Sftp: Error while downloading backup to sftp server. Check server details in config.yml (ip, port, user, password).");
        defaults.accept("Error.SftpLocalDeletionFailed", "Sftp: Local backup deletion failed because the uploaded file was not found on the sftp server. Try again.");
        defaults.accept("Error.SftpNotFound", "Sftp: sftp-backup %file% not found.");
        defaults.accept("Error.SftpConnectionFailed", "Sftp: Error while connecting to SFTP server.");
        defaults.accept("Error.SmbUploadFailed", "SMB: Upload failed. Check the SMB host, share, credentials, and permissions.");
        defaults.accept("Error.SmbLocalDeletionFailed", "SMB: Upload succeeded, but the local backup could not be deleted.");
        defaults.accept("Error.NoSmbBackups", "No SMB backups found.");
        defaults.accept("Error.SmbNotFound", "SMB backup %file% not found.");
        defaults.accept("Error.SmbDownloadFailed", "SMB: Download failed. Check the SMB connection and permissions.");

    }
}

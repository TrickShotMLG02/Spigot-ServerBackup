package net.server_backup.commands;

import net.server_backup.Configuration;
import net.server_backup.ServerBackup;
import net.server_backup.core.CompletionService;
import net.server_backup.utils.FtpManager;
import net.server_backup.utils.SftpManager;
import net.server_backup.utils.SmbManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Collects platform-specific candidates; remote listings are cached asynchronously. */
public class TabCompleter implements org.bukkit.command.TabCompleter {
    private final ServerBackup plugin;
    public TabCompleter(ServerBackup plugin) { this.plugin = plugin; }
    private final Map<String, List<String>> remoteCache = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshAt = new ConcurrentHashMap<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new CompletionService(new CompletionService.Port() {
            public List<String> worlds() {
                List<String> result = new ArrayList<>();
                for (World world : Bukkit.getWorlds()) result.add(Bukkit.getWorldContainer().getPath().equals(".")
                        ? world.getName() : new File(Bukkit.getWorldContainer(), world.getName()).getPath());
                return result;
            }
            public List<String> localFiles() {
                File[] files = new File(plugin.configuration().backupDestination).listFiles();
                if (files == null) return Collections.emptyList();
                List<String> result = new ArrayList<>();
                for (File file : files) result.add(file.getName());
                return result;
            }
            public List<String> localRegularFiles() {
                File[] files = new File(plugin.configuration().backupDestination).listFiles(File::isFile);
                if (files == null) return Collections.emptyList();
                List<String> result = new ArrayList<>();
                for (File file : files) result.add(file.getName());
                return result;
            }
            public List<String> remoteFiles(String provider) {
                if (System.currentTimeMillis() >= refreshAt.getOrDefault(provider, 0L) && loading.add(provider)) {
                    plugin.platform().runAsync(() -> {
                        try {
                            List<String> names = new ArrayList<>();
                            if (provider.equals("ftp")) {
                                for (String item : new FtpManager(plugin, sender).getFtpBackupList(true)) names.add(item.substring(0, item.lastIndexOf(':')));
                            } else if (provider.equals("sftp")) {
                                for (String item : new SftpManager(plugin, sender).getSftpBackupList(true)) {
                                    String path = item.substring(0, item.lastIndexOf(':')).replace('\\', '/');
                                    names.add(path.substring(path.lastIndexOf('/') + 1));
                                }
                            } else {
                                for (String item : new SmbManager(plugin, sender).getSmbBackupList()) names.add(item.substring(0, item.lastIndexOf(' ')));
                            }
                            remoteCache.put(provider, names);
                            refreshAt.put(provider, System.currentTimeMillis() + 30000);
                        } finally { loading.remove(provider); }
                    });
                }
                return remoteCache.getOrDefault(provider, Collections.emptyList());
            }
        }).complete(sender.hasPermission("backup.admin"), args);
    }
}

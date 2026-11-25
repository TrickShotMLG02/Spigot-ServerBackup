package net.server_backup.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Command-completion decisions independent of the command framework. */
public final class CompletionService {
    public interface Port {
        List<String> worlds();
        List<String> localFiles();
        List<String> localRegularFiles();
        List<String> remoteFiles(String provider);
    }
    private final Port port;
    public CompletionService(Port port) { this.port = port; }
    public List<String> complete(boolean permitted, String[] args) {
        if (!permitted || args.length == 0) return Collections.emptyList();
        List<String> candidates = new ArrayList<>();
        String command = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) candidates.addAll(List.of("reload", "list", "search", "create", "remove", "zip", "unzip", "ftp", "sftp", "smb", "dropbox", "tasks", "shutdown"));
        else if (args.length == 2) {
            switch (command) {
                case "list": for (int i = 1; i <= (port.localFiles().size() + 9) / 10; i++) candidates.add(Integer.toString(i)); break;
                case "remove": candidates.addAll(port.localFiles()); break;
                case "zip": for (String name : port.localFiles()) if (!name.endsWith(".zip")) candidates.add(name); break;
                case "unzip": for (String name : port.localFiles()) if (name.endsWith(".zip")) candidates.add(name); break;
                case "create": candidates.addAll(port.worlds()); candidates.add("@server"); break;
                case "ftp": case "sftp": case "smb": candidates.addAll(List.of("list", "download", "upload")); break;
                case "dropbox": candidates.add("upload"); break;
                default: break;
            }
        } else if (args.length == 3) {
            if (command.equals("create")) candidates.add("-full");
            else if (List.of("ftp", "sftp", "smb").contains(command) && args[1].equalsIgnoreCase("download")) candidates.addAll(port.remoteFiles(command));
            else if (List.of("ftp", "sftp", "smb", "dropbox").contains(command) && args[1].equalsIgnoreCase("upload")) {
                for (String name : command.equals("smb") ? port.localRegularFiles() : port.localFiles())
                    if (command.equals("smb") || name.endsWith(".zip")) candidates.add(name);
            }
        } else if (command.equals("create")) candidates.add("-full");
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        candidates.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(prefix));
        Collections.sort(candidates);
        return candidates;
    }
}

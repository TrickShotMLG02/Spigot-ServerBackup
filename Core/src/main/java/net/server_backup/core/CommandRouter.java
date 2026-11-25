package net.server_backup.core;

import java.util.Locale;

/** Maps the existing backup command syntax to platform-independent actions. */
public final class CommandRouter {
    public enum Action { DENIED, HELP, NONE, CREATE, SHUTDOWN, RELOAD, TASKS, LIST, SEARCH, ZIP, UNZIP, REMOVE, FTP, SFTP, SMB, DROPBOX }
    public static final class Route {
        public final Action action;
        public final String[] arguments;
        private Route(Action action, String[] arguments) { this.action = action; this.arguments = arguments; }
    }
    public Route route(boolean permitted, String[] args) {
        if (!permitted) return new Route(Action.DENIED, args);
        if (args.length == 0) return new Route(Action.HELP, args);
        String command = args[0].toLowerCase(Locale.ROOT);
        if (command.equals("create") && args.length >= 2) return new Route(Action.CREATE, args);
        if (args.length == 1) {
            switch (command) {
                case "shutdown": return new Route(Action.SHUTDOWN, args);
                case "reload": case "rl": return new Route(Action.RELOAD, args);
                case "tasks": case "task": return new Route(Action.TASKS, args);
                case "list": return new Route(Action.LIST, new String[]{"list", "1"});
                default: return new Route(Action.HELP, args);
            }
        }
        if (args.length == 2) {
            switch (command) {
                case "list": return new Route(Action.LIST, args);
                case "search": return new Route(Action.SEARCH, new String[]{"search", args[1], "1"});
                case "zip": return new Route(Action.ZIP, args);
                case "unzip": return new Route(Action.UNZIP, args);
                case "delete": case "remove": return new Route(Action.REMOVE, args);
                case "ftp": case "sftp": case "smb":
                    if (args[1].equalsIgnoreCase("list")) return new Route(Action.valueOf(command.toUpperCase(Locale.ROOT)), new String[]{command, "list", "1"});
                    return new Route(Action.NONE, args);
                default: return new Route(Action.NONE, args);
            }
        }
        if (args.length == 3) {
            switch (command) {
                case "search": return new Route(Action.SEARCH, args);
                case "ftp": return new Route(Action.FTP, args);
                case "sftp": return new Route(Action.SFTP, args);
                case "smb": return new Route(Action.SMB, args);
                case "dropbox": return new Route(Action.DROPBOX, args);
                default: return new Route(Action.NONE, args);
            }
        }
        return new Route(Action.HELP, args);
    }
}

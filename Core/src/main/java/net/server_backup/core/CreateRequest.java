package net.server_backup.core;

import java.nio.file.Files;
import java.nio.file.Paths;

/** Parses the existing create command, including names containing spaces. */
public final class CreateRequest {
    public final String source;
    public final boolean full;
    private CreateRequest(String source, boolean full) { this.source = source; this.full = full; }
    public static CreateRequest parse(String[] args) {
        if (args.length < 2) throw new IllegalArgumentException("Missing backup source");
        StringBuilder source = new StringBuilder(args[1]);
        boolean full = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-full")) full = true;
            else source.append(' ').append(args[i]);
        }
        return new CreateRequest(source.toString(), full);
    }
    public boolean isSingleFile() { return !source.equalsIgnoreCase("@server") && !Files.isDirectory(Paths.get(source)); }
}

package net.server_backup.bootstrap;

import java.util.Locale;
import java.util.function.Predicate;

/** Selects the Bukkit-family adapter without linking Paper classes. */
public final class PlatformSelection {
    private PlatformSelection() { }
    public static boolean isPaper(String serverName, Predicate<String> classAvailable) {
        return serverName.toLowerCase(Locale.ROOT).contains("paper")
                || classAvailable.test("io.papermc.paper.configuration.Configuration");
    }
}

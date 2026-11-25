package net.server_backup.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandRouterTest {
    private final CommandRouter router = new CommandRouter();
    @Test void routesAliasesDefaultsAndPermission() {
        assertEquals(CommandRouter.Action.DENIED, router.route(false, new String[]{"create", "world"}).action);
        assertEquals(CommandRouter.Action.RELOAD, router.route(true, new String[]{"rl"}).action);
        CommandRouter.Route list = router.route(true, new String[]{"ftp", "list"});
        assertEquals(CommandRouter.Action.FTP, list.action);
        assertArrayEquals(new String[]{"ftp", "list", "1"}, list.arguments);
        assertArrayEquals(new String[]{"search", "world", "1"}, router.route(true, new String[]{"search", "world"}).arguments);
        assertEquals(CommandRouter.Action.CREATE, router.route(true, new String[]{"create", "my", "world", "-full"}).action);
    }
}

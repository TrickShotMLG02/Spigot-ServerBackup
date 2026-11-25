package net.server_backup.bootstrap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlatformSelectionTest {
    @Test void detectsPaperByNameOrCapabilityAndKeepsSpigotIndependent() {
        assertTrue(PlatformSelection.isPaper("Paper", ignored -> false));
        assertTrue(PlatformSelection.isPaper("Purpur", name -> name.equals("io.papermc.paper.configuration.Configuration")));
        assertFalse(PlatformSelection.isPaper("Spigot", ignored -> false));
    }
    @Test void entryPointAndPaperAdapterLoadWithoutPaperApi() throws Exception {
        ClassLoader withoutPaper = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.papermc.")) throw new ClassNotFoundException(name);
                if (name.startsWith("net.server_backup.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            try (java.io.InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                                if (in == null) throw new ClassNotFoundException(name);
                                byte[] bytes = in.readAllBytes();
                                loaded = defineClass(name, bytes, 0, bytes.length);
                            } catch (java.io.IOException e) { throw new ClassNotFoundException(name, e); }
                        }
                        if (resolve) resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
        assertNotNull(Class.forName("net.server_backup.bootstrap.ServerBackupBootstrap", false, withoutPaper));
        assertNotNull(Class.forName("net.server_backup.platform.paper.PaperPlatform", false, withoutPaper));
        assertThrows(ClassNotFoundException.class, () -> withoutPaper.loadClass("io.papermc.paper.configuration.Configuration"));
    }
}

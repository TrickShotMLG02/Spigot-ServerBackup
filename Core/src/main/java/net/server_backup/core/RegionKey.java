package net.server_backup.core;

import java.nio.file.Path;

/** Legacy backupInfo.yml key for a region containing a chunk. */
public final class RegionKey {
    private RegionKey() { }

    public static String forChunk(Path worldContainer, String world, int chunkX, int chunkZ) {
        Path worldPath = worldContainer.resolve(world).normalize();
        return "Data." + worldPath + ".Chunk.r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca";
    }
}

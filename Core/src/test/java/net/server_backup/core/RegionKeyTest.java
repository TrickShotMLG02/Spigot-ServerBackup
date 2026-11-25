package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class RegionKeyTest {
    @Test void mapsPositiveAndNegativeChunksToLegacyRegionKeys() {
        assertEquals("Data.world.Chunk.r.0.1.mca", RegionKey.forChunk(Paths.get("."), "world", 31, 32));
        assertEquals("Data.world.Chunk.r.-1.-2.mca", RegionKey.forChunk(Paths.get("."), "world", -1, -33));
    }
}

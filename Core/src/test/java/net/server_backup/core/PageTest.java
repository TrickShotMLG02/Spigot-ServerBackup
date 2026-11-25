package net.server_backup.core;

import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class PageTest {
    @Test void slicesAndValidatesPages() {
        var values = IntStream.range(0, 12).boxed().toList();
        Page<Integer> page = Page.of(values, 2);
        assertEquals(2, page.pages());
        assertEquals(java.util.List.of(10, 11), page.items);
        assertThrows(IllegalArgumentException.class, () -> Page.of(values, 0));
        assertThrows(IllegalArgumentException.class, () -> Page.of(values, 3));
    }
}

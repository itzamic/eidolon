package io.github.itzamic.eidolon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EidolonConfigTest {

    @Test
    void normalizeContextPathVariants() {
        // Leading slash added
        EidolonConfig c1 = EidolonConfig.builder().contextPath("foo").build();
        assertEquals("/foo", c1.contextPath());

        // Trailing slash removed
        EidolonConfig c2 = EidolonConfig.builder().contextPath("/bar/").build();
        assertEquals("/bar", c2.contextPath());

        // Root stays root
        EidolonConfig c3 = EidolonConfig.builder().contextPath("/").build();
        assertEquals("/", c3.contextPath());
    }

    @Test
    void builderDefaultsAndSetters() {
        EidolonConfig defaults = EidolonConfig.builder().build();
        assertTrue(defaults.enabled());
        assertEquals("0.0.0.0", defaults.host());
        assertEquals(7090, defaults.port());
        assertEquals("/eidolon", defaults.contextPath());
        assertTrue(defaults.websocketBroadcastEnabled());
        assertEquals(1000L, defaults.broadcastIntervalMillis());
        assertEquals(1024, defaults.gcEventBufferSize());
        assertEquals(false, defaults.collectStringTable());

        EidolonConfig custom = EidolonConfig.builder()
                .enabled(true)
                .host("127.0.0.1")
                .port(8080)
                .contextPath("/metrics/")
                .websocketBroadcastEnabled(false)
                .broadcastIntervalMillis(2000L)
                .gcEventBufferSize(256)
                .collectStringTable(true)
                .build();

        assertEquals("127.0.0.1", custom.host());
        assertEquals(8080, custom.port());
        // normalization removes trailing slash
        assertEquals("/metrics", custom.contextPath());
        assertEquals(false, custom.websocketBroadcastEnabled());
        assertEquals(2000L, custom.broadcastIntervalMillis());
        assertEquals(256, custom.gcEventBufferSize());
        assertEquals(true, custom.collectStringTable());
    }
}

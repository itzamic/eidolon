package io.github.itzamic.eidolon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class EidolonTest {

    @Test
    void startStopLifecycle() {
        // ensure clean state
        Eidolon.stop();

        EidolonConfig cfg = EidolonConfig.builder()
                .enabled(true)
                .host("127.0.0.1")
                .port(0) // random free port
                .contextPath("/eidolon")
                .websocketBroadcastEnabled(false) // simplify test
                .collectStringTable(false)
                .build();

        Eidolon.start(cfg);
        assertTrue(Eidolon.isRunning(), "Eidolon should report running after start");

        Eidolon.stop();
        assertFalse(Eidolon.isRunning(), "Eidolon should not be running after stop");

        // idempotent stop
        Eidolon.stop();
        assertFalse(Eidolon.isRunning(), "Subsequent stops should be no-ops");
    }

    @Test
    void doesNotStartWhenDisabled() {
        // ensure clean state
        Eidolon.stop();

        EidolonConfig disabled = EidolonConfig.builder()
                .enabled(false)
                .build();

        Eidolon.start(disabled);
        assertFalse(Eidolon.isRunning(), "Disabled config should not start the server");
    }
}

package io.github.itzamic.eidolon.service;

import io.github.itzamic.eidolon.EidolonConfig;
import io.github.itzamic.eidolon.model.MetricsSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsServiceTest {

    @Test
    void snapshotProducesReasonableData() {
        EidolonConfig cfg = EidolonConfig.builder()
                .enabled(true)
                .collectStringTable(false)
                .build();

        MetricsService svc = new MetricsService(cfg);
        MetricsSnapshot snap = svc.snapshot();

        assertNotNull(snap, "snapshot should not be null");
        assertTrue(snap.timestampMillis > 0, "timestamp should be > 0");

        assertNotNull(snap.heap, "heap should not be null");
        assertTrue(snap.heap.used >= -1);
        assertTrue(snap.heap.committed >= -1);
        assertTrue(snap.heap.max >= -1);

        assertNotNull(snap.threads, "threads should not be null");
        assertTrue(snap.threads.threadCount >= 0);
        assertNotNull(snap.threads.stateCounts);

        assertNotNull(snap.classes, "classes should not be null");
        assertTrue(snap.classes.loadedClassCount >= 0);
        assertTrue(snap.classes.totalLoadedClassCount >= 0);
        assertTrue(snap.classes.unloadedClassCount >= 0);

        assertNotNull(snap.recentGcEvents, "gc events list should not be null");
    }
}

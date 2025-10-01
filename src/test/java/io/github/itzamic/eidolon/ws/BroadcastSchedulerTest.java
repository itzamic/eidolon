package io.github.itzamic.eidolon.ws;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.itzamic.eidolon.model.MetricsSnapshot;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Classes;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Heap;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Threads;
import io.github.itzamic.eidolon.service.MetricsService;
import io.micronaut.json.JsonMapper;

class BroadcastSchedulerTest {

    private static MetricsSnapshot sampleSnapshot() {
        Heap heap = new Heap(1, 2, 3, List.of());
        Threads threads = new Threads(1, 1, 1, 1L, Map.of());
        Classes classes = new Classes(1, 1, 0);
        return new MetricsSnapshot(0L, heap, threads, classes, null, List.of());
    }

    @Test
    void noClientsDoesNotBroadcast() throws Exception {
        SessionRegistry registry = Mockito.mock(SessionRegistry.class);
        MetricsService metrics = Mockito.mock(MetricsService.class);
        JsonMapper json = Mockito.mock(JsonMapper.class);

        when(registry.size()).thenReturn(0);

        // interval 10ms to keep the test quick
        BroadcastScheduler scheduler = new BroadcastScheduler(registry, metrics, json, 10L);

        assertDoesNotThrow(scheduler::start);
        // wait a bit and ensure no broadcast happened
        Thread.sleep(60);
        assertDoesNotThrow(scheduler::stop);

        verify(registry, never()).broadcast(anyString());
    }

    @Test
    void withClientsBroadcastsPeriodically() throws Exception {
        SessionRegistry registry = Mockito.mock(SessionRegistry.class);
        MetricsService metrics = Mockito.mock(MetricsService.class);
        JsonMapper json = Mockito.mock(JsonMapper.class);

        when(registry.size()).thenReturn(1);
        when(metrics.snapshot()).thenReturn(sampleSnapshot());
        byte[] payload = "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8);
        when(json.writeValueAsBytes(any())).thenReturn(payload);

        BroadcastScheduler scheduler = new BroadcastScheduler(registry, metrics, json, 10L);

        assertDoesNotThrow(scheduler::start);
        // allow a few ticks to occur
        Thread.sleep(120);
        assertDoesNotThrow(scheduler::stop);

        verify(registry, atLeastOnce()).broadcast(new String(payload, StandardCharsets.UTF_8));
    }
}

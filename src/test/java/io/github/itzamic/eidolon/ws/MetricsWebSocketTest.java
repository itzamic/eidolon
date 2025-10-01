package io.github.itzamic.eidolon.ws;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.itzamic.eidolon.model.MetricsSnapshot;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Classes;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Heap;
import io.github.itzamic.eidolon.model.MetricsSnapshot.Threads;
import io.github.itzamic.eidolon.service.MetricsService;
import io.micronaut.json.JsonMapper;
import io.micronaut.websocket.WebSocketSession;

class MetricsWebSocketTest {

    private static MetricsSnapshot sampleSnapshot() {
        Heap heap = new Heap(1, 2, 3, List.of());
        Threads threads = new Threads(1, 1, 1, 1L, Map.of());
        Classes classes = new Classes(1, 1, 0);
        return new MetricsSnapshot(0L, heap, threads, classes, null, List.of());
    }

    @Test
    void onOpenRegistersAndSendsInitialSnapshot() throws Exception {
        SessionRegistry registry = mock(SessionRegistry.class);
        MetricsService metrics = mock(MetricsService.class);
        JsonMapper json = mock(JsonMapper.class);
        WebSocketSession session = mock(WebSocketSession.class);

        MetricsSnapshot snap = sampleSnapshot();
        when(metrics.snapshot()).thenReturn(snap);
        byte[] payload = "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8);
        when(json.writeValueAsBytes(any())).thenReturn(payload);

        MetricsWebSocket ws = new MetricsWebSocket(registry, metrics, json);

        assertDoesNotThrow(() -> ws.onOpen(session));

        verify(registry, times(1)).add(session);
        verify(session, times(1)).sendSync(new String(payload, StandardCharsets.UTF_8));
    }

    @Test
    void onMessagePingAndSnapshot() throws Exception {
        SessionRegistry registry = mock(SessionRegistry.class);
        MetricsService metrics = mock(MetricsService.class);
        JsonMapper json = mock(JsonMapper.class);
        WebSocketSession session = mock(WebSocketSession.class);

        MetricsSnapshot snap = sampleSnapshot();
        when(metrics.snapshot()).thenReturn(snap);
        byte[] payload = "{\"s\":\"v\"}".getBytes(StandardCharsets.UTF_8);
        when(json.writeValueAsBytes(any())).thenReturn(payload);

        MetricsWebSocket ws = new MetricsWebSocket(registry, metrics, json);

        ws.onMessage("ping", session);
        verify(session, times(1)).sendSync("pong");

        ws.onMessage("snapshot", session);
        verify(metrics, times(1)).snapshot();
        verify(session, times(1)).sendSync(new String(payload, StandardCharsets.UTF_8));

        // Unknown command should not throw
        assertDoesNotThrow(() -> ws.onMessage("unknown", session));
    }

    @Test
    void onCloseRemovesSession() {
        SessionRegistry registry = mock(SessionRegistry.class);
        MetricsService metrics = mock(MetricsService.class);
        JsonMapper json = mock(JsonMapper.class);
        WebSocketSession session = mock(WebSocketSession.class);

        MetricsWebSocket ws = new MetricsWebSocket(registry, metrics, json);
        ws.onClose(session);

        verify(registry, times(1)).remove(session);
    }
}

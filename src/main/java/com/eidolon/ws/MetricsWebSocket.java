package com.eidolon.ws;

import com.eidolon.model.MetricsSnapshot;
import com.eidolon.service.MetricsService;
import io.micronaut.json.JsonMapper;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;

@ServerWebSocket("/ws/metrics")
public class MetricsWebSocket {

    private final SessionRegistry registry;
    private final MetricsService metrics;
    private final JsonMapper json;

    @Inject
    public MetricsWebSocket(SessionRegistry registry, MetricsService metrics, JsonMapper json) {
        this.registry = registry;
        this.metrics = metrics;
        this.json = json;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        registry.add(session);
        // Send initial snapshot
        try {
            MetricsSnapshot snap = metrics.snapshot();
            byte[] bytes = json.writeValueAsBytes(snap);
            session.sendSync(new String(bytes, StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        // Optional simple protocol:
        // - "snapshot" -> push a snapshot immediately
        // - "ping" -> "pong"
        try {
            if ("ping".equalsIgnoreCase(message)) {
                session.sendSync("pong");
            } else if ("snapshot".equalsIgnoreCase(message)) {
                MetricsSnapshot snap = metrics.snapshot();
                byte[] bytes = json.writeValueAsBytes(snap);
                session.sendSync(new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        registry.remove(session);
    }
}

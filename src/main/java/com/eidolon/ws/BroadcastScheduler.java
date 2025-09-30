package com.eidolon.ws;

import com.eidolon.service.MetricsService;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically broadcasts metrics snapshots to connected WebSocket clients
 * when eidolon.websocket.enabled=true.
 * This implementation uses a local ScheduledExecutorService to avoid depending
 * on Micronaut's scheduling module.
 */
@Singleton
@Requires(property = "eidolon.websocket.enabled", value = "true")
public class BroadcastScheduler {

    private final SessionRegistry registry;
    private final MetricsService metrics;
    private final JsonMapper json;
    private final long intervalMs;

    private ScheduledExecutorService executor;

    @Inject
    public BroadcastScheduler(SessionRegistry registry,
                              MetricsService metrics,
                              JsonMapper json,
                              @Value("${eidolon.websocket.interval:1000}") long intervalMs) {
        this.registry = registry;
        this.metrics = metrics;
        this.json = json;
        this.intervalMs = intervalMs;
    }

    @PostConstruct
    void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "eidolon-ws-broadcast");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::tick, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (registry.size() == 0) {
            return;
        }
        try {
            byte[] payload = json.writeValueAsBytes(metrics.snapshot());
            registry.broadcast(new String(payload, StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}

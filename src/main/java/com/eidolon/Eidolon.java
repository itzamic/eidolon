package com.eidolon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;

/**
 * Entry point to start/stop the embedded Micronaut server that exposes JVM metrics.
 * Can be used programmatically or via the Java agent (see EidolonAgent).
 */
public final class Eidolon {

    private static final AtomicReference<State> STATE = new AtomicReference<>(null);

    private Eidolon() {}

    public static synchronized void startDefault() {
        EidolonConfig.Builder b = EidolonConfig.builder();

        String host = System.getProperty("eidolon.host");
        if (host != null && !host.isEmpty()) {
            b.host(host);
        }
        String portStr = System.getProperty("eidolon.port");
        if (portStr != null) {
            try {
                b.port(Integer.parseInt(portStr));
            } catch (NumberFormatException ignored) {
            }
        }
        String ctx = System.getProperty("eidolon.contextPath");
        if (ctx != null && !ctx.isEmpty()) {
            b.contextPath(ctx);
        }
        String wsEnabled = System.getProperty("eidolon.websocket.enabled");
        if (wsEnabled != null) {
            b.websocketBroadcastEnabled(Boolean.parseBoolean(wsEnabled));
        }
        String interval = System.getProperty("eidolon.websocket.interval");
        if (interval != null) {
            try {
                b.broadcastIntervalMillis(Long.parseLong(interval));
            } catch (NumberFormatException ignored) {
            }
        }
        String buf = System.getProperty("eidolon.gc.bufferSize");
        if (buf != null) {
            try {
                b.gcEventBufferSize(Integer.parseInt(buf));
            } catch (NumberFormatException ignored) {
            }
        }
        String strTbl = System.getProperty("eidolon.collect.stringTable");
        if (strTbl != null) {
            b.collectStringTable(Boolean.parseBoolean(strTbl));
        }

        start(b.build());
    }

    public static synchronized void start(EidolonConfig config) {
        Objects.requireNonNull(config, "config");
        if (!config.enabled()) {
            return;
        }
        if (STATE.get() != null) {
            return; // already started
        }

        Map<String, Object> props = new HashMap<>();
        props.put("micronaut.server.host", config.host());
        props.put("micronaut.server.port", config.port());
        // Use Micronaut's context-path to mount all controllers/websockets under a base path.
        props.put("micronaut.server.context-path", config.contextPath());

        // Export selected config values for beans to use (simple property mapping).
        props.put("eidolon.websocket.enabled", config.websocketBroadcastEnabled());
        props.put("eidolon.websocket.interval", config.broadcastIntervalMillis());
        props.put("eidolon.gc.bufferSize", config.gcEventBufferSize());
        props.put("eidolon.collect.stringTable", config.collectStringTable());

        ApplicationContext context = ApplicationContext.builder(props)
                .singletons(config) // expose EidolonConfig as a bean
                .build()
                .start();

        EmbeddedServer server = context.getBean(EmbeddedServer.class);
        if (!server.isRunning()) {
            server.start();
        }

        // Ensure clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(Eidolon::stop, "eidolon-shutdown"));

        STATE.set(new State(context, server));
    }

    public static synchronized void stop() {
        State s = STATE.getAndSet(null);
        if (s != null) {
            try {
                if (s.server != null && s.server.isRunning()) {
                    s.server.stop();
                }
            } finally {
                if (s.context != null && s.context.isRunning()) {
                    s.context.close();
                }
            }
        }
    }

    public static boolean isRunning() {
        State s = STATE.get();
        return s != null && s.server != null && s.server.isRunning();
    }

    private record State(ApplicationContext context, EmbeddedServer server) {}
}

package io.github.itzamic.eidolon.api;

import io.github.itzamic.eidolon.EidolonConfig;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsControllerTest {

    @Test
    void snapshotEndpointResponds() {
        ApplicationContext context = null;
        EmbeddedServer server = null;
        HttpClient client = null;
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("micronaut.server.host", "127.0.0.1");
            props.put("micronaut.server.port", 0); // random free port
            props.put("micronaut.server.context-path", "/eidolon");
            props.put("eidolon.websocket.enabled", false);
            props.put("eidolon.collect.stringTable", false);

            EidolonConfig cfg = EidolonConfig.builder()
                    .enabled(true)
                    .host("127.0.0.1")
                    .port(0)
                    .contextPath("/eidolon")
                    .websocketBroadcastEnabled(false)
                    .collectStringTable(false)
                    .build();

            context = ApplicationContext.builder(props).singletons(cfg).build().start();

            server = context.getBean(EmbeddedServer.class);
            if (!server.isRunning()) {
                server.start();
            }

            URL baseUrl = server.getURL();
            client = HttpClient.create(baseUrl);

            HttpResponse<String> resp = client.toBlocking()
                    .exchange(HttpRequest.GET("/eidolon/api/metrics/snapshot"), String.class);

            assertEquals(200, resp.getStatus().getCode(), "snapshot endpoint should return 200");
            String body = resp.body();
            assertNotNull(body, "snapshot body should not be null");
            assertTrue(body.contains("\"heap\""), "snapshot body should contain heap object");
            assertTrue(body.contains("\"threads\""), "snapshot body should contain threads object");
            assertTrue(body.contains("\"classes\""), "snapshot body should contain classes object");
        } catch (Exception e) {
            fail("snapshot endpoint test failed: " + e.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {}
            }
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception ignored) {}
            }
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ignored) {}
            }
        }
    }
}

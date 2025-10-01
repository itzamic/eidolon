package io.github.itzamic.eidolon.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

import io.github.itzamic.eidolon.EidolonConfig;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;

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

            // Additional endpoints
            HttpResponse<String> heapResp = client.toBlocking()
                    .exchange(HttpRequest.GET("/eidolon/api/metrics/heap"), String.class);
            assertEquals(200, heapResp.getStatus().getCode(), "heap endpoint should return 200");
            assertNotNull(heapResp.body(), "heap body should not be null");
            assertTrue(heapResp.body().contains("used"), "heap body should contain 'used'");

            HttpResponse<String> threadsResp = client.toBlocking()
                    .exchange(HttpRequest.GET("/eidolon/api/metrics/threads"), String.class);
            assertEquals(200, threadsResp.getStatus().getCode(), "threads endpoint should return 200");
            assertNotNull(threadsResp.body(), "threads body should not be null");
            assertTrue(threadsResp.body().contains("threadCount"), "threads body should contain 'threadCount'");

            HttpResponse<String> classesResp = client.toBlocking()
                    .exchange(HttpRequest.GET("/eidolon/api/metrics/classes"), String.class);
            assertEquals(200, classesResp.getStatus().getCode(), "classes endpoint should return 200");
            assertNotNull(classesResp.body(), "classes body should not be null");
            assertTrue(classesResp.body().contains("loadedClassCount"), "classes body should contain 'loadedClassCount'");

            // When collection disabled, controller returns null -> Micronaut responds 404 Not Found by default.
            // Accept either 404 (preferred) or 200 with literal "null" depending on framework behavior/version.
            try {
                HttpResponse<String> stringTableResp = client.toBlocking()
                        .exchange(HttpRequest.GET("/eidolon/api/metrics/string-table"), String.class);
                int code = stringTableResp.getStatus().getCode();
                if (code == 200) {
                    assertNotNull(stringTableResp.body(), "string-table body should not be null when 200");
                    assertEquals("null", stringTableResp.body().trim(), "string-table should be null when collection is disabled");
                } else {
                    assertEquals(404, code, "string-table endpoint should return 404 when body is null");
                }
            } catch (io.micronaut.http.client.exceptions.HttpClientResponseException e) {
                assertEquals(404, e.getStatus().getCode(), "string-table endpoint should return 404 when body is null");
            }

            HttpResponse<String> gcEventsResp = client.toBlocking()
                    .exchange(HttpRequest.GET("/eidolon/api/metrics/gc/events"), String.class);
            assertEquals(200, gcEventsResp.getStatus().getCode(), "gc events endpoint should return 200");
            assertNotNull(gcEventsResp.body(), "gc events body should not be null");
            assertTrue(gcEventsResp.body().trim().startsWith("["), "gc events body should be a JSON array");
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

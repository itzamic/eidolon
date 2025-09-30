package io.github.itzamic.eidolon.api;

import io.github.itzamic.eidolon.model.MetricsSnapshot;
import io.github.itzamic.eidolon.service.MetricsService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

import java.util.List;

@Controller("/api/metrics")
public class MetricsController {

    private final MetricsService metrics;

    @Inject
    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @Get(uri = "/snapshot", produces = MediaType.APPLICATION_JSON)
    public MetricsSnapshot snapshot() {
        return metrics.snapshot();
    }

    @Get(uri = "/heap", produces = MediaType.APPLICATION_JSON)
    public MetricsSnapshot.Heap heap() {
        return metrics.snapshot().heap;
    }

    @Get(uri = "/threads", produces = MediaType.APPLICATION_JSON)
    public MetricsSnapshot.Threads threads() {
        return metrics.snapshot().threads;
    }

    @Get(uri = "/classes", produces = MediaType.APPLICATION_JSON)
    public MetricsSnapshot.Classes classes() {
        return metrics.snapshot().classes;
    }

    @Get(uri = "/string-table", produces = MediaType.APPLICATION_JSON)
    public MetricsSnapshot.StringTable stringTable() {
        return metrics.snapshot().stringTable;
    }

    @Get(uri = "/gc/events", produces = MediaType.APPLICATION_JSON)
    public List<MetricsSnapshot.GcEvent> gcEvents() {
        return metrics.snapshot().recentGcEvents;
    }
}

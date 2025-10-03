package io.github.itzamic.eidolon.api;

import io.github.itzamic.eidolon.model.RuntimeInfo;
import io.github.itzamic.eidolon.service.RuntimeInfoService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

@Controller("/api/runtime")
public class RuntimeController {

    private final RuntimeInfoService runtime;

    @Inject
    public RuntimeController(RuntimeInfoService runtime) {
        this.runtime = runtime;
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public RuntimeInfo runtimeInfo() {
        return runtime.getRuntimeInfo();
    }
}

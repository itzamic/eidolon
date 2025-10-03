package io.github.itzamic.eidolon.service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import io.github.itzamic.eidolon.EidolonConfig;
import io.github.itzamic.eidolon.model.RuntimeInfo;
import jakarta.inject.Singleton;

@Singleton
public class RuntimeInfoService {

    private final EidolonConfig config;

    public RuntimeInfoService(EidolonConfig config) {
        this.config = config;
    }

    public RuntimeInfo getRuntimeInfo() {
        RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean mmx = ManagementFactory.getMemoryMXBean();
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();

        // JVM/runtime info
        String jvmName = System.getProperty("java.runtime.name");
        String jvmVendor = System.getProperty("java.vendor");
        String jvmVersion = System.getProperty("java.runtime.version", System.getProperty("java.version"));

        String vmName = rmx.getVmName();
        String vmVendor = rmx.getVmVendor();
        String vmVersion = rmx.getVmVersion();

        List<String> gcCollectors = gcs.stream().map(GarbageCollectorMXBean::getName).toList();
        List<String> inputArguments = rmx.getInputArguments();

        long init = mmx.getHeapMemoryUsage().getInit();
        long max = mmx.getHeapMemoryUsage().getMax();
        Long heapInit = init < 0 ? null : init;
        Long heapMax = max < 0 ? null : max;

        return new RuntimeInfo(
                jvmName, jvmVendor, jvmVersion,
                vmName, vmVendor, vmVersion,
                gcCollectors, inputArguments, heapInit, heapMax,
                config.host(), config.port(), config.contextPath(),
                config.websocketBroadcastEnabled(), config.broadcastIntervalMillis(),
                config.gcEventBufferSize(), config.collectStringTable()
        );
    }
}

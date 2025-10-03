package io.github.itzamic.eidolon.model;

import java.util.List;

/**
 * Immutable DTO with runtime/JVM and Eidolon configuration info.
 */
public final class RuntimeInfo {
    // JVM/runtime
    public final String jvmName;
    public final String jvmVendor;
    public final String jvmVersion;
    public final String vmName;
    public final String vmVendor;
    public final String vmVersion;

    // GC/runtime args and heap sizing
    public final List<String> gcCollectors;
    public final List<String> inputArguments;
    public final Long heapInit;
    public final Long heapMax;

    // Eidolon config
    public final String host;
    public final int port;
    public final String contextPath;
    public final boolean websocketEnabled;
    public final long websocketIntervalMillis;
    public final int gcEventBufferSize;
    public final boolean collectStringTable;

    public RuntimeInfo(
            String jvmName,
            String jvmVendor,
            String jvmVersion,
            String vmName,
            String vmVendor,
            String vmVersion,
            List<String> gcCollectors,
            List<String> inputArguments,
            Long heapInit,
            Long heapMax,
            String host,
            int port,
            String contextPath,
            boolean websocketEnabled,
            long websocketIntervalMillis,
            int gcEventBufferSize,
            boolean collectStringTable
    ) {
        this.jvmName = jvmName;
        this.jvmVendor = jvmVendor;
        this.jvmVersion = jvmVersion;
        this.vmName = vmName;
        this.vmVendor = vmVendor;
        this.vmVersion = vmVersion;
        this.gcCollectors = gcCollectors;
        this.inputArguments = inputArguments;
        this.heapInit = heapInit;
        this.heapMax = heapMax;
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.websocketEnabled = websocketEnabled;
        this.websocketIntervalMillis = websocketIntervalMillis;
        this.gcEventBufferSize = gcEventBufferSize;
        this.collectStringTable = collectStringTable;
    }
}

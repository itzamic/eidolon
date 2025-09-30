package io.github.itzamic.eidolon.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable DTOs for metrics returned by REST/WebSocket.
 * Fields are public final for zero-boilerplate Jackson serialization.
 */
public final class MetricsSnapshot {
    public final long timestampMillis;
    public final Heap heap;
    public final Threads threads;
    public final Classes classes;
    public final StringTable stringTable; // may be null if not available or disabled
    public final List<GcEvent> recentGcEvents;

    public MetricsSnapshot(long timestampMillis,
                           Heap heap,
                           Threads threads,
                           Classes classes,
                           StringTable stringTable,
                           List<GcEvent> recentGcEvents) {
        this.timestampMillis = timestampMillis;
        this.heap = heap;
        this.threads = threads;
        this.classes = classes;
        this.stringTable = stringTable;
        this.recentGcEvents = recentGcEvents;
    }

    public static final class Heap {
        public final long used;
        public final long committed;
        public final long max;
        public final List<MemoryPool> pools;

        public Heap(long used, long committed, long max, List<MemoryPool> pools) {
            this.used = used;
            this.committed = committed;
            this.max = max;
            this.pools = pools;
        }
    }

    public static final class MemoryPool {
        public final String name;
        public final String type;
        public final Usage usage;
        public final Usage collectionUsage; // may be null on some JVMs

        public MemoryPool(String name, String type, Usage usage, Usage collectionUsage) {
            this.name = name;
            this.type = type;
            this.usage = usage;
            this.collectionUsage = collectionUsage;
        }
    }

    public static final class Usage {
        public final long init;
        public final long used;
        public final long committed;
        public final long max;

        public Usage(long init, long used, long committed, long max) {
            this.init = init;
            this.used = used;
            this.committed = committed;
            this.max = max;
        }
    }

    public static final class Threads {
        public final int threadCount;
        public final int daemonThreadCount;
        public final int peakThreadCount;
        public final long totalStartedThreadCount;
        public final Map<String, Integer> stateCounts;

        public Threads(int threadCount, int daemonThreadCount, int peakThreadCount, long totalStartedThreadCount, Map<String, Integer> stateCounts) {
            this.threadCount = threadCount;
            this.daemonThreadCount = daemonThreadCount;
            this.peakThreadCount = peakThreadCount;
            this.totalStartedThreadCount = totalStartedThreadCount;
            this.stateCounts = stateCounts;
        }
    }

    public static final class Classes {
        public final long loadedClassCount;
        public final long totalLoadedClassCount;
        public final long unloadedClassCount;

        public Classes(long loadedClassCount, long totalLoadedClassCount, long unloadedClassCount) {
            this.loadedClassCount = loadedClassCount;
            this.totalLoadedClassCount = totalLoadedClassCount;
            this.unloadedClassCount = unloadedClassCount;
        }
    }

    public static final class StringTable {
        public final boolean available;
        public final Long tableSize;
        public final Long bucketCount;
        public final Long entryCount;
        public final Long totalMemoryBytes;
        public final Map<String, Object> rawAttributes; // any additional vendor attrs

        public StringTable(boolean available, Long tableSize, Long bucketCount, Long entryCount, Long totalMemoryBytes, Map<String, Object> rawAttributes) {
            this.available = available;
            this.tableSize = tableSize;
            this.bucketCount = bucketCount;
            this.entryCount = entryCount;
            this.totalMemoryBytes = totalMemoryBytes;
            this.rawAttributes = rawAttributes;
        }
    }

    public static final class GcEvent {
        public final String gcName;
        public final String gcAction;
        public final String gcCause;
        public final long startTimeMillis;
        public final long durationMillis;

        public GcEvent(String gcName, String gcAction, String gcCause, long startTimeMillis, long durationMillis) {
            this.gcName = gcName;
            this.gcAction = gcAction;
            this.gcCause = gcCause;
            this.startTimeMillis = startTimeMillis;
            this.durationMillis = durationMillis;
        }
    }
}

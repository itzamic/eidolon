package io.github.itzamic.eidolon.service;

import io.github.itzamic.eidolon.EidolonConfig;
import io.github.itzamic.eidolon.model.MetricsSnapshot;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class MetricsService {

    private final EidolonConfig config;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

    private final ArrayDeque<MetricsSnapshot.GcEvent> gcEvents;
    private final List<NotificationEmitter> registeredEmitters = new ArrayList<>();
    private final NotificationListener gcListener = this::onGcNotification;

    public MetricsService(EidolonConfig config) {
        this.config = config;
        this.gcEvents = new ArrayDeque<>(Math.max(16, config.gcEventBufferSize()));
    }

    @PostConstruct
    void init() {
        // Register for GC notifications if available
        final List<java.lang.management.GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (java.lang.management.GarbageCollectorMXBean gc : gcs) {
            if (gc instanceof NotificationEmitter emitter) {
                try {
                    emitter.addNotificationListener(gcListener, null, null);
                    registeredEmitters.add(emitter);
                } catch (Exception ignored) {
                    // ignore registration failures
                }
            }
        }
    }

    @PreDestroy
    void shutdown() {
        for (NotificationEmitter emitter : registeredEmitters) {
            try {
                emitter.removeNotificationListener(gcListener);
            } catch (Exception ignored) {
            }
        }
        registeredEmitters.clear();
        gcEvents.clear();
    }

    private void onGcNotification(Notification notification, Object handback) {
        // Filter by type constant to avoid reflective dependency on com.sun.* in signatures
        if (!"com.sun.management.gc.notification".equals(notification.getType())) {
            return;
        }
        try {
            // payload is a CompositeData with GarbageCollectionNotificationInfo
            // We avoid compile-time dependency; parse defensively via standard MXBean views
            Object userData = notification.getUserData();
            // Fallback: use com.sun.management API reflectively if present
            Class<?> gcInfoCls = Class.forName("com.sun.management.GarbageCollectionNotificationInfo");
            Class<?> comDataCls = Class.forName("javax.management.openmbean.CompositeData");
            Object gcNotif = gcInfoCls.getMethod("from", comDataCls).invoke(null, userData);
            String gcName = (String) gcInfoCls.getMethod("getGcName").invoke(gcNotif);
            if (!config.includeGcNames().isEmpty() && !config.includeGcNames().contains(gcName)) {
                return;
            }
            String gcAction = (String) gcInfoCls.getMethod("getGcAction").invoke(gcNotif);
            String gcCause = (String) gcInfoCls.getMethod("getGcCause").invoke(gcNotif);
            Object gcInfo = gcInfoCls.getMethod("getGcInfo").invoke(gcNotif);
            long startTime = (Long) gcInfo.getClass().getMethod("getStartTime").invoke(gcInfo);
            long duration = (Long) gcInfo.getClass().getMethod("getDuration").invoke(gcInfo);

            MetricsSnapshot.GcEvent evt = new MetricsSnapshot.GcEvent(gcName, gcAction, gcCause, startTime, duration);
            synchronized (gcEvents) {
                gcEvents.addLast(evt);
                while (gcEvents.size() > config.gcEventBufferSize()) {
                    gcEvents.removeFirst();
                }
            }
        } catch (Throwable ignored) {
            // If reflective parsing fails, skip event
        }
    }

    public MetricsSnapshot snapshot() {
        long now = Instant.now().toEpochMilli();

        // Heap summary
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        long used = safeLong(heap.getUsed());
        long committed = safeLong(heap.getCommitted());
        long max = safeLong(heap.getMax());

        // Pools (optional filtering)
        List<MetricsSnapshot.MemoryPool> poolDtos = new ArrayList<>();
        for (MemoryPoolMXBean p : memoryPools) {
            if (!config.includeMemoryPools().isEmpty() && !config.includeMemoryPools().contains(p.getName())) {
                continue;
            }
            MemoryUsage usage = p.getUsage();
            MemoryUsage coll = p.getCollectionUsage();
            MetricsSnapshot.Usage u = usage == null ? null : new MetricsSnapshot.Usage(
                    safeLong(usage.getInit()), safeLong(usage.getUsed()), safeLong(usage.getCommitted()), safeLong(usage.getMax())
            );
            MetricsSnapshot.Usage cu = coll == null ? null : new MetricsSnapshot.Usage(
                    safeLong(coll.getInit()), safeLong(coll.getUsed()), safeLong(coll.getCommitted()), safeLong(coll.getMax())
            );
            String type = p.getType() == null ? "UNKNOWN" : (p.getType() == MemoryType.HEAP ? "HEAP" : "NON_HEAP");
            poolDtos.add(new MetricsSnapshot.MemoryPool(p.getName(), type, u, cu));
        }

        MetricsSnapshot.Heap heapDto = new MetricsSnapshot.Heap(used, committed, max, poolDtos);

        // Threads
        int threadCount = threadMXBean.getThreadCount();
        int daemonThreadCount = threadMXBean.getDaemonThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        long totalStarted = threadMXBean.getTotalStartedThreadCount();
        Map<String, Integer> stateCounts = computeThreadStateCounts();

        MetricsSnapshot.Threads threadsDto = new MetricsSnapshot.Threads(
                threadCount, daemonThreadCount, peakThreadCount, totalStarted, stateCounts
        );

        // Classes
        long loaded = classLoadingMXBean.getLoadedClassCount();
        long totalLoaded = classLoadingMXBean.getTotalLoadedClassCount();
        long unloaded = classLoadingMXBean.getUnloadedClassCount();
        MetricsSnapshot.Classes classesDto = new MetricsSnapshot.Classes(loaded, totalLoaded, unloaded);

        // String table (optional)
        MetricsSnapshot.StringTable stringTableDto = null;
        if (config.collectStringTable()) {
            stringTableDto = readStringTable();
        }

        // Recent GC events
        List<MetricsSnapshot.GcEvent> gcCopy;
        synchronized (gcEvents) {
            gcCopy = new ArrayList<>(gcEvents);
        }

        return new MetricsSnapshot(now, heapDto, threadsDto, classesDto, stringTableDto, gcCopy);
    }

    private Map<String, Integer> computeThreadStateCounts() {
        Map<Thread.State, Integer> counts = new EnumMap<>(Thread.State.class);
        for (Thread.State s : Thread.State.values()) counts.put(s, 0);
        try {
            long[] ids = threadMXBean.getAllThreadIds();
            Thread.State[] states = new Thread.State[ids.length];
            for (int i = 0; i < ids.length; i++) {
                try {
                    java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(ids[i], 0);
                    if (info != null) {
                        boolean include = config.includeThreadNamePrefixes().isEmpty();
                        if (!include) {
                            String tn = info.getThreadName();
                            if (tn != null) {
                                for (String prefix : config.includeThreadNamePrefixes()) {
                                    if (tn.startsWith(prefix)) {
                                        include = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (include) {
                            states[i] = info.getThreadState();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            for (Thread.State st : states) {
                if (st != null) counts.put(st, counts.get(st) + 1);
            }
        } catch (Exception ignored) {
        }
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<Thread.State, Integer> e : counts.entrySet()) {
            out.put(e.getKey().name(), e.getValue());
        }
        return out;
    }

    private MetricsSnapshot.StringTable readStringTable() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName on = new ObjectName("java.lang:type=StringTable");
            // Probe quickly if available
            if (!server.isRegistered(on)) {
                return new MetricsSnapshot.StringTable(false, null, null, null, null, Collections.emptyMap());
            }

            // Collect a set of common attributes across vendors
            String[] attrs = new String[] {
                    "TableSize", "BucketCount", "Size", "EntryCount", "Entries", "TotalMemory", "MemoryUsage", "RehashCount"
            };
            AttributeList list = server.getAttributes(on, attrs);

            Long tableSize = null, bucketCount = null, entryCount = null, totalBytes = null;
            Map<String, Object> raw = new HashMap<>();
            for (Object obj : list) {
                if (obj instanceof Attribute a) {
                    String name = a.getName();
                    Object val = a.getValue();
                    raw.put(name, val);
                    if (val instanceof Number n) {
                        switch (name) {
                            case "TableSize", "Size" -> tableSize = n.longValue();
                            case "BucketCount" -> bucketCount = n.longValue();
                            case "EntryCount" -> entryCount = n.longValue();
                            case "TotalMemory", "MemoryUsage" -> totalBytes = n.longValue();
                            default -> {}
                        }
                    }
                }
            }
            return new MetricsSnapshot.StringTable(true, tableSize, bucketCount, entryCount, totalBytes, raw);
        } catch (Throwable t) {
            return new MetricsSnapshot.StringTable(false, null, null, null, null, Collections.emptyMap());
        }
    }

    private static long safeLong(long v) {
        return v < 0 ? -1 : v;
    }
}

package com.eidolon;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for the embedded Eidolon server and metrics collection.
 */
public final class EidolonConfig {
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String contextPath;
    private final boolean websocketBroadcastEnabled;
    private final long broadcastIntervalMillis;
    private final int gcEventBufferSize;
    private final boolean collectStringTable;

    // Optional simple filters (allow-lists). Empty means no filtering.
    private final Set<String> includeMemoryPools;
    private final Set<String> includeGcNames;
    private final Set<String> includeThreadNamePrefixes;

    private EidolonConfig(Builder b) {
        this.enabled = b.enabled;
        this.host = b.host;
        this.port = b.port;
        this.contextPath = normalizeContextPath(b.contextPath);
        this.websocketBroadcastEnabled = b.websocketBroadcastEnabled;
        this.broadcastIntervalMillis = b.broadcastIntervalMillis;
        this.gcEventBufferSize = b.gcEventBufferSize;
        this.collectStringTable = b.collectStringTable;
        this.includeMemoryPools = Collections.unmodifiableSet(b.includeMemoryPools);
        this.includeGcNames = Collections.unmodifiableSet(b.includeGcNames);
        this.includeThreadNamePrefixes = Collections.unmodifiableSet(b.includeThreadNamePrefixes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean enabled() {
        return enabled;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String contextPath() {
        return contextPath;
    }

    public boolean websocketBroadcastEnabled() {
        return websocketBroadcastEnabled;
    }

    public long broadcastIntervalMillis() {
        return broadcastIntervalMillis;
    }

    public int gcEventBufferSize() {
        return gcEventBufferSize;
    }

    public boolean collectStringTable() {
        return collectStringTable;
    }

    public Set<String> includeMemoryPools() {
        return includeMemoryPools;
    }

    public Set<String> includeGcNames() {
        return includeGcNames;
    }

    public Set<String> includeThreadNamePrefixes() {
        return includeThreadNamePrefixes;
    }

    private static String normalizeContextPath(String cp) {
        if (cp == null || cp.isEmpty() || "/".equals(cp)) {
            return "/";
        }
        String s = cp.startsWith("/") ? cp : "/" + cp;
        if (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static final class Builder {
        private boolean enabled = true;
        private String host = "0.0.0.0";
        private int port = 7090;
        private String contextPath = "/eidolon";
        private boolean websocketBroadcastEnabled = true;
        private long broadcastIntervalMillis = 1000L;
        private int gcEventBufferSize = 1024;
        private boolean collectStringTable = false;

        private Set<String> includeMemoryPools = Collections.emptySet();
        private Set<String> includeGcNames = Collections.emptySet();
        private Set<String> includeThreadNamePrefixes = Collections.emptySet();

        private Builder() {}

        public Builder enabled(boolean v) {
            this.enabled = v;
            return this;
        }

        public Builder host(String v) {
            this.host = Objects.requireNonNull(v, "host");
            return this;
        }

        public Builder port(int v) {
            this.port = v;
            return this;
        }

        public Builder contextPath(String v) {
            this.contextPath = Objects.requireNonNull(v, "contextPath");
            return this;
        }

        public Builder websocketBroadcastEnabled(boolean v) {
            this.websocketBroadcastEnabled = v;
            return this;
        }

        public Builder broadcastIntervalMillis(long v) {
            this.broadcastIntervalMillis = v;
            return this;
        }

        public Builder gcEventBufferSize(int v) {
            this.gcEventBufferSize = v;
            return this;
        }

        public Builder collectStringTable(boolean v) {
            this.collectStringTable = v;
            return this;
        }

        public Builder includeMemoryPools(Set<String> v) {
            this.includeMemoryPools = v == null ? Collections.emptySet() : v;
            return this;
        }

        public Builder includeGcNames(Set<String> v) {
            this.includeGcNames = v == null ? Collections.emptySet() : v;
            return this;
        }

        public Builder includeThreadNamePrefixes(Set<String> v) {
            this.includeThreadNamePrefixes = v == null ? Collections.emptySet() : v;
            return this;
        }

        public EidolonConfig build() {
            return new EidolonConfig(this);
        }
    }
}

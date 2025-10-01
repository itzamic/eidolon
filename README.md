# Eidolon - JVM Introspection Plugin

[![CI](https://github.com/itzamic/eidolon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/itzamic/eidolon/actions/workflows/ci.yml)

Eidolon is a lightweight Java library and Java Agent that embeds a Micronaut HTTP/WebSocket server inside any JVM process to expose real-time metrics for live monitoring.

Core features:
- Heap usage and memory pools
- GC events (via JMX notifications)
- Thread activity and state counts
- Loaded classes statistics
- Optional String Table stats (when available)
- REST and WebSocket APIs
- Configurable, low-overhead collection
- Works automatically via -javaagent (no code changes needed)

--------------------------------------------------------------------------------

1) Getting Started

Security note: Eidolon endpoints are unauthenticated and intended for local development or protected environments. Do not expose them publicly without adding authentication/proxy controls.

Option A: Run as Java Agent (no code changes)
- Build/publish the library (see section 4).
- Add the built JAR as a -javaagent to your JVM (application server, CLI app, etc.):
  -javaagent:/path/to/eidolon-0.1.0-SNAPSHOT.jar
- Optional system properties:
  -Deidolon.host=0.0.0.0
  -Deidolon.port=7090
  -Deidolon.contextPath=/eidolon
  -Deidolon.websocket.enabled=true
  -Deidolon.websocket.interval=1000
  -Deidolon.gc.bufferSize=1024
  -Deidolon.collect.stringTable=false

Option B: Programmatic start (one line of code)
- Add dependency (see section 4).
- Start the embedded server early in your main or bootstrap:
  io.github.itzamic.eidolon.Eidolon.startDefault();

2) Endpoints

Base path: controlled by context-path (default /eidolon). The API lives under /api/metrics and WS under /ws/metrics.

- HTTP (JSON):
  - GET {contextPath}/api/metrics/snapshot
    Full snapshot with heap, pools, threads, classes, gc events, optional string table.
  - GET {contextPath}/api/metrics/heap
  - GET {contextPath}/api/metrics/threads
  - GET {contextPath}/api/metrics/classes
  - GET {contextPath}/api/metrics/string-table
  - GET {contextPath}/api/metrics/gc/events

- WebSocket:
  - WS {contextPath}/ws/metrics
    - On open: sends a snapshot immediately.
    - Messages:
      - "ping" -> responds "pong"
      - "snapshot" -> sends a fresh snapshot

Default configuration:
- Host: 0.0.0.0
- Port: 7090
- Context path: /eidolon
- WebSocket broadcast interval: 1000 ms (configurable/disable by property)
- GC event buffer size: 1024
- String Table collection: disabled by default (enable with -Deidolon.collect.stringTable=true)

3) What’s included

- Embedded Micronaut server bootstrap (Eidolon class)
- Programmatic config (EidolonConfig)
- REST controllers (MetricsController)
- Metrics collection service (MetricsService) using MXBeans and GC JMX notifications
- WebSocket live feed + broadcast scheduler
- Java Agent (EidolonAgent) to start without code changes
- Gradle maven-publish configuration for easy distribution

4) Build and Publish

Build:
- ./gradlew clean build -x test

Publish to local Maven (so other projects can depend on it):
- ./gradlew publishToMavenLocal

The artifact coordinates default to:
- groupId: io.github.itzamic
- artifactId: eidolon
- version: 0.1.0-SNAPSHOT

Use in another Gradle project:
repositories {
  mavenLocal()
  mavenCentral()
}
dependencies {
  implementation("io.github.itzamic:eidolon:0.1.0-SNAPSHOT")
}

Run with the agent:
- Add JVM arg:
  -javaagent:$HOME/.m2/repository/io/github/itzamic/eidolon/0.1.0-SNAPSHOT/eidolon-0.1.0-SNAPSHOT.jar

5) Configuration

You can pass configuration either via:
- Java system properties (-D...)
- Programmatic builder using EidolonConfig.builder() and Eidolon.start(config)

Supported keys (system properties) consumed by Eidolon (library):
- eidolon.host (default 0.0.0.0)
- eidolon.port (default 7090)
- eidolon.contextPath (default /eidolon)
- eidolon.websocket.enabled (true/false, default true)
- eidolon.websocket.interval (milliseconds, default 1000)
- eidolon.gc.bufferSize (int, default 1024)
- eidolon.collect.stringTable (true/false, default false)

Note: The sample app additionally uses the property eidolon.disableProgrammaticStart=true to avoid double-starting when running with -javaagent. This property is specific to the sample app’s main class and is not read by the library.

Optional filters (programmatic-only for now):
- includeMemoryPools(Set<String>)
- includeGcNames(Set<String>)
- includeThreadNamePrefixes(Set<String>)

6) Data Model (JSON)

High-level shape of /api/metrics/snapshot:
{
  "timestampMillis": 1699999999999,
  "heap": {
    "used": 1234567,
    "committed": 2345678,
    "max": 3456789,
    "pools": [
      {
        "name": "G1 Eden Space",
        "type": "HEAP",
        "usage": { "init": ..., "used": ..., "committed": ..., "max": ... },
        "collectionUsage": { ... or null ... }
      }
    ]
  },
  "threads": {
    "threadCount": 42,
    "daemonThreadCount": 20,
    "peakThreadCount": 100,
    "totalStartedThreadCount": 1234,
    "stateCounts": { "RUNNABLE": 10, "WAITING": 5, ... }
  },
  "classes": {
    "loadedClassCount": 1234,
    "totalLoadedClassCount": 5678,
    "unloadedClassCount": 4444
  },
  "stringTable": {
    "available": true,
    "tableSize": 8192,
    "bucketCount": ...,
    "entryCount": ...,
    "totalMemoryBytes": ...,
    "rawAttributes": { "VendorSpecific": ... }
  },
  "recentGcEvents": [
    {
      "gcName": "G1 Young Generation",
      "gcAction": "end of minor GC",
      "gcCause": "G1 Evacuation Pause",
      "startTimeMillis": 123,
      "durationMillis": 7
    }
  ]
}

7) Example client usage

HTTP example (curl):
- curl http://localhost:7090/eidolon/api/metrics/snapshot | jq

WebSocket example (plain JS):
const sock = new WebSocket("ws://localhost:7090/eidolon/ws/metrics");
sock.onopen = () => console.log("WS open");
sock.onmessage = (ev) => console.log("metrics", JSON.parse(ev.data));

8) Optional Next.js dashboard example (minimal snippet)

Example page (app/page.tsx or pages/index.tsx) to show a live heap chart:

"use client";
import { useEffect, useRef, useState } from "react";

export default function Home() {
  const [points, setPoints] = useState<{ t: number, used: number }[]>([]);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:7090/eidolon/ws/metrics");
    wsRef.current = ws;
    ws.onmessage = (ev) => {
      try {
        const snap = JSON.parse(ev.data);
        setPoints((prev) => [...prev.slice(-299), { t: snap.timestampMillis, used: snap.heap.used }]);
      } catch {}
    };
    return () => ws.close();
  }, []);

  return (
    <div style={{ padding: 20 }}>
      <h1>Eidolon Heap Usage</h1>
      <div>Points: {points.length}</div>
      <svg width={600} height={200} style={{ border: "1px solid #ccc" }}>
        {points.map((p, i) => {
          const x = (i / 300) * 600;
          const y = 200 - Math.min(200, (p.used / 100_000_000) * 200);
          return <circle key={i} cx={x} cy={y} r={2} fill="teal" />;
        })}
      </svg>
    </div>
  );
}

9) Notes and Best Practices

- Overhead is minimal: metrics are obtained via MXBeans and notifications; broadcast interval is configurable.
- GC notifications are parsed reflectively from com.sun.management. If unavailable, GC events are skipped quietly.
- StringTable MBean isn’t guaranteed across all JVMs; the code handles its absence gracefully.
- Security: this ships without auth. Do not expose endpoints publicly without adding auth/proxy controls.
- Modular design: the metrics service and transport layers are decoupled. Future extensions can add CPU/alloc profiling.

10) Integration summary

- As a dependency only: call Eidolon.startDefault(); OR run with -javaagent for no-code start.
- As a Java Agent: best path to “automatic” start with zero code changes.
- Configure via -D properties typed above.

11) Development

- Build: ./gradlew build
- Local publish: ./gradlew publishToMavenLocal
- Test with a sample app:
  - Add dependency implementation("io.github.itzamic:eidolon:0.1.0-SNAPSHOT")
  - Add JVM arg: -javaagent:~/.m2/repository/io/github/itzamic/eidolon/0.1.0-SNAPSHOT/eidolon-0.1.0-SNAPSHOT.jar
  - Open: http://localhost:7090/eidolon/api/metrics/snapshot
  - WS: ws://localhost:7090/eidolon/ws/metrics

Compatibility
- JDK: Built and tested with JDK 21 (Gradle toolchain enforces 21 by default).

--------------------------------------------------------------------------------

License: Apache-2.0 (default in POM; adjust as needed)

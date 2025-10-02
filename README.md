# Eidolon - JVM Introspection Plugin

[![Backend CI](https://github.com/itzamic/eidolon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/itzamic/eidolon/actions/workflows/ci.yml) [![Frontend CI](https://github.com/itzamic/eidolon/actions/workflows/frontend-ci.yml/badge.svg?branch=main)](https://github.com/itzamic/eidolon/actions/workflows/frontend-ci.yml)

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

Security note: Eidolon endpoints are unauthenticated and intended for local development or protected environments. Do not expose them publicly without adding authentication/proxy controls.

## Repository structure

- Backend (library + agent): Micronaut HTTP/WS server exposing JVM metrics
  - src/main/java/io/github/itzamic/eidolon
- Frontend: Next.js 14 example dashboard to visualize live metrics
  - frontend/
- Sample app: minimal Java app to try Eidolon quickly (programmatic and agent modes)
  - test/sample-app/

--------------------------------------------------------------------------------

## 1) Getting Started

Option A: Run as Java Agent (no code changes)
- Build/publish the library (see section 5).
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
- Add dependency (see section 5).
- Start the embedded server early in your main or bootstrap:
  io.github.itzamic.eidolon.Eidolon.startDefault();

Stop the server programmatically if needed:
- io.github.itzamic.eidolon.Eidolon.stop();

--------------------------------------------------------------------------------

## 2) Endpoints

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
    - Periodic broadcast: when enabled (default true), snapshots are pushed on an interval. See eidolon.websocket.enabled and eidolon.websocket.interval.

Default configuration:
- Host: 0.0.0.0
- Port: 7090
- Context path: /eidolon
- WebSocket broadcast interval: 1000 ms (configurable / can be disabled)
- GC event buffer size: 1024
- String Table collection: disabled by default (enable with -Deidolon.collect.stringTable=true)

--------------------------------------------------------------------------------

## 3) Data Model (JSON)

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

--------------------------------------------------------------------------------

## 4) Example usage

HTTP example (curl):
- curl http://localhost:7090/eidolon/api/metrics/snapshot | jq

WebSocket example (plain JS):
const sock = new WebSocket("ws://localhost:7090/eidolon/ws/metrics");
sock.onopen = () => console.log("WS open");
sock.onmessage = (ev) => console.log("metrics", JSON.parse(ev.data));

--------------------------------------------------------------------------------

## 5) Build and Publish

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

--------------------------------------------------------------------------------

## 6) Configuration

You can pass configuration either via:
- Java system properties (-D...)
- Programmatic builder using EidolonConfig.builder() and Eidolon.start(config)

Supported keys (system properties) consumed by Eidolon:
- eidolon.host (default 0.0.0.0)
- eidolon.port (default 7090)
- eidolon.contextPath (default /eidolon)
- eidolon.websocket.enabled (true/false, default true)
- eidolon.websocket.interval (milliseconds, default 1000)
- eidolon.gc.bufferSize (int, default 1024)
- eidolon.collect.stringTable (true/false, default false)

Programmatic builder (extra optional filters are programmatic-only):
- includeMemoryPools(Set<String>)
- includeGcNames(Set<String>)
- includeThreadNamePrefixes(Set<String>)

Notes:
- When websocket broadcast is disabled, clients still receive an initial snapshot on open and can request on-demand snapshots using the "snapshot" WS message.
- The sample app additionally uses the property eidolon.disableProgrammaticStart=true to avoid double-starting when running with -javaagent. This property is specific to the sample app and is not read by the library itself.

--------------------------------------------------------------------------------

## 7) Frontend dashboard (Next.js) — included

This repository includes a minimal dashboard that visualizes live metrics.

Prerequisites:
- Node.js 18+ (or 20+ recommended)
- Backend running locally (see sections above or the sample app)

Dev setup:
1) Start a JVM with Eidolon on port 7090 and contextPath /eidolon (the defaults).
2) In a new terminal:
   cd frontend
   npm install
   npm run dev
3) Open http://localhost:3000

Configuration and rewrites:
- The Next.js dev server proxies HTTP requests on /eidolon/* to http://localhost:7090/eidolon/* (see frontend/next.config.mjs).
- WebSockets are NOT proxied by Next rewrites. The frontend connects directly by default to:
  ws://localhost:7090/eidolon/ws/metrics
- You can override endpoints via environment variables:
  - NEXT_PUBLIC_EIDOLON_BASE (default: /eidolon) — HTTP base path relative to the frontend
  - NEXT_PUBLIC_EIDOLON_WS_URL (default: ws://localhost:7090/eidolon/ws/metrics)

Where things live:
- Frontend pages/components live under frontend/src
- Connection configuration is in frontend/src/lib/config.ts

--------------------------------------------------------------------------------

## 8) Sample app

A minimal Java app is provided under test/sample-app to try Eidolon quickly.

Publish Eidolon locally, then run one of:
- Programmatic start (Eidolon.startDefault):
  ./gradlew -p test/sample-app runProgrammatic
- Agent mode (-javaagent, programmatic start disabled):
  ./gradlew -p test/sample-app runAgent

On startup you should see URLs like:
- HTTP: http://localhost:7090/eidolon/api/metrics/snapshot
- WS:   ws://localhost:7090/eidolon/ws/metrics

See test/sample-app/README.md for more.

--------------------------------------------------------------------------------

## 9) Development notes

- Build: ./gradlew build
- Tests: ./gradlew test
- Coverage report: ./gradlew jacocoTestReport
  - HTML report: build/reports/jacocoHtml/index.html
- Local publish: ./gradlew publishToMavenLocal

Implementation details:
- Micronaut server configuration is set via ApplicationContext properties.
- WebSocket periodic broadcast is provided by a simple ScheduledExecutorService (no Micronaut scheduler dependency) and gated by eidolon.websocket.enabled.
- GC notifications are parsed reflectively from com.sun.management. If unavailable, GC events are skipped quietly.
- StringTable MBean isn’t guaranteed across all JVMs; absence is handled gracefully.

--------------------------------------------------------------------------------

## 10) Compatibility

- JDK: Built and tested with JDK 21 (Gradle toolchain enforces 21 by default).

--------------------------------------------------------------------------------

## 11) License

License: Apache-2.0 (configured in POM)

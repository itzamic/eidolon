# Eidolon Sample App (Test Project)

Minimal Java app to demonstrate using the Eidolon library in two ways:
- Programmatic start (Eidolon.startDefault())
- Java Agent (-javaagent) with programmatic start disabled

This app also simulates periodic heap allocations so metrics change over time.

## Project Layout

test/sample-app
- settings.gradle.kts
- build.gradle.kts
- src/main/java/com/example/App.java

## Prerequisites

- JDK 21+
- Use the Gradle wrapper at the repository root

## 1) Publish Eidolon to your local Maven

From the repository root (where the main build.gradle.kts is):

./gradlew clean build publishToMavenLocal

This publishes the Eidolon artifact to ~/.m2/repository so the sample app can depend on it.

Coordinates used by the sample:
- io.github.itzamic:eidolon:0.1.0-SNAPSHOT

## 2) Run the Sample App

Use the wrapper from the repository root and target the sample project with -p:

- Programmatic start (Eidolon.startDefault):
  ./gradlew -p test/sample-app runProgrammatic

- Agent mode (-javaagent, programmatic start disabled):
  ./gradlew -p test/sample-app runAgent

On startup you should see URLs like:
- HTTP: http://localhost:7090/eidolon/api/metrics/snapshot
- WS:   ws://localhost:7090/eidolon/ws/metrics

Press Ctrl+C to stop.

## 3) Manual agent run (optional)

If you prefer to run manually:

1) Build the sample app jar (optional; Gradle JavaExec tasks are already set up):
   ./gradlew -p test/sample-app build

2) Run with -javaagent and disable programmatic start:
   java \
     -javaagent:$HOME/.m2/repository/io/github/itzamic/eidolon/0.1.0-SNAPSHOT/eidolon-0.1.0-SNAPSHOT.jar \
     -Deidolon.disableProgrammaticStart=true \
     -cp test/sample-app/build/classes/java/main \
     com.example.App

## 4) Override Configuration

Pass -D system properties to change defaults:
- eidolon.host (default 0.0.0.0)
- eidolon.port (default 7090)
- eidolon.contextPath (default /eidolon)
- eidolon.websocket.enabled (true/false, default true)
- eidolon.websocket.interval (milliseconds, default 1000)
- eidolon.gc.bufferSize (int, default 1024)
- eidolon.collect.stringTable (true/false, default false)

Examples:
- Programmatic:
  ./gradlew -p test/sample-app runProgrammatic --args="" -Deidolon.port=8081 -Deidolon.contextPath=/eidolon

- Agent:
  ./gradlew -p test/sample-app runAgent -Deidolon.port=8082 -Deidolon.contextPath=/eidolon

Note: In agent mode, the task already sets -Deidolon.disableProgrammaticStart=true.

## 5) Try the Endpoints

- Full snapshot (JSON):
  curl http://localhost:7090/eidolon/api/metrics/snapshot | jq

- Specific slices:
  curl http://localhost:7090/eidolon/api/metrics/heap | jq
  curl http://localhost:7090/eidolon/api/metrics/threads | jq
  curl http://localhost:7090/eidolon/api/metrics/classes | jq
  curl http://localhost:7090/eidolon/api/metrics/string-table | jq
  curl http://localhost:7090/eidolon/api/metrics/gc/events | jq

- WebSocket quick test (browser console):
  const sock = new WebSocket("ws://localhost:7090/eidolon/ws/metrics");
  sock.onopen = () => console.log("WS open");
  sock.onmessage = (ev) => console.log("metrics", JSON.parse(ev.data));

## 6) Troubleshooting

- Dependency not found:
  - Re-run: ./gradlew publishToMavenLocal at the repository root
  - Ensure the version in test/sample-app/build.gradle.kts matches what was published (0.1.0-SNAPSHOT by default)

- Port already in use:
  - Choose a different port with -Deidolon.port=7091

- No changing metrics:
  - The app allocates/deallocates memory periodically; leave it running a bit to observe heap/GC changes.

## Notes

- Programmatic vs Agent:
  - Programmatic: App calls Eidolon.startDefault() directly.
  - Agent: Eidolon starts automatically via -javaagent; the app disables its programmatic start via -Deidolon.disableProgrammaticStart=true.

- Security:
  - Endpoints are unauthenticated for local testing. Do not expose them publicly without adding auth/proxy controls.

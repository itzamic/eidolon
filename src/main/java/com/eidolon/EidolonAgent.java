package com.eidolon;

import java.lang.instrument.Instrumentation;

/**
 * Java agent entrypoints to bootstrap Eidolon without any code change
 * in the host application. Use with:
 *
 *   -javaagent:/path/to/eidolon.jar
 *
 * Optional system properties to configure:
 *   -Deidolon.host=0.0.0.0
 *   -Deidolon.port=7090
 *   -Deidolon.contextPath=/eidolon
 *   -Deidolon.websocket.enabled=true
 *   -Deidolon.websocket.interval=1000
 *   -Deidolon.gc.bufferSize=1024
 *   -Deidolon.collect.stringTable=false
 *
 * The agent does not instrument classes; it only starts the embedded Micronaut server.
 */
public final class EidolonAgent {

    private EidolonAgent() {}

    /**
     * Invoked when loaded via -javaagent at JVM startup.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        bootstrap(agentArgs);
    }

    /**
     * Invoked when attached dynamically to a running JVM (if using tools that support it).
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        bootstrap(agentArgs);
    }

    private static void bootstrap(String agentArgs) {
        // agentArgs is currently ignored; configuration is via system properties (see javadoc)
        try {
            Eidolon.startDefault();
        } catch (Throwable t) {
            // Swallow exceptions to avoid disrupting host process startup
            t.printStackTrace(System.err);
        }
    }
}

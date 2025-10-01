package com.example;

import io.github.itzamic.eidolon.Eidolon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Minimal console app that demonstrates using Eidolon:
 * - Programmatic start: call Eidolon.startDefault()
 * - Agent mode: start with -javaagent and disable programmatic start via -Deidolon.disableProgrammaticStart=true
 *
 * It also simulates heap churn so metrics are visibly changing over time.
 */
public class App {
    public static void main(String[] args) throws Exception {
        boolean disableProgrammatic = Boolean.getBoolean("eidolon.disableProgrammaticStart");
        if (!disableProgrammatic) {
            Eidolon.startDefault();
        }

        String port = System.getProperty("eidolon.port", "7090");
        String contextPath = System.getProperty("eidolon.contextPath", "/eidolon");

        System.out.println("Eidolon sample app started.");
        System.out.println("HTTP: http://localhost:" + port + contextPath + "/api/metrics/snapshot");
        System.out.println("WS:   ws://localhost:" + port + contextPath + "/ws/metrics");

        // Simulate memory churn so heap/GC metrics change over time
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "simulated-load");
            t.setDaemon(true);
            return t;
        });

        List<byte[]> ballast = new ArrayList<>();
        ses.scheduleAtFixedRate(() -> {
            try {
                // Allocate ~2MB per tick; occasionally drop half to trigger GC activity
                ballast.add(new byte[2 * 1024 * 1024]);
                if (ballast.size() % 8 == 0) {
                    int remove = ballast.size() / 2;
                    ballast.subList(0, remove).clear();
                }
            } catch (OutOfMemoryError oom) {
                ballast.clear();
            }
        }, 1, 1, TimeUnit.SECONDS);

        // Keep the process alive
        Thread.currentThread().join();
    }
}

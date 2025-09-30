package io.github.itzamic.eidolon.ws;

import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SessionRegistry {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void add(WebSocketSession session) {
        sessions.add(session);
    }

    public void remove(WebSocketSession session) {
        sessions.remove(session);
    }

    public int size() {
        return sessions.size();
    }

    public void broadcast(String message) {
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendSync(message);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}

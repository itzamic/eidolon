package io.github.itzamic.eidolon.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micronaut.websocket.WebSocketSession;

class SessionRegistryTest {

    @Test
    void addRemoveAndSize() {
        SessionRegistry reg = new SessionRegistry();
        assertEquals(0, reg.size(), "initial size should be 0");

        WebSocketSession s1 = mock(WebSocketSession.class);
        when(s1.isOpen()).thenReturn(true);

        reg.add(s1);
        assertEquals(1, reg.size(), "size should be 1 after add");

        reg.remove(s1);
        assertEquals(0, reg.size(), "size should be 0 after remove");
    }

    @Test
    void broadcastSendsToOpenSessionsAndIgnoresClosedOrErrors() {
        SessionRegistry reg = new SessionRegistry();

        WebSocketSession open = mock(WebSocketSession.class);
        when(open.isOpen()).thenReturn(true);

        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);

        WebSocketSession broken = mock(WebSocketSession.class);
        when(broken.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(broken).sendSync(anyString());

        reg.add(open);
        reg.add(closed);
        reg.add(broken);

        String msg = "hello";
        reg.broadcast(msg);

        verify(open, times(1)).sendSync(msg);
        verify(closed, never()).sendSync(anyString());
        // broken will be attempted and exception is swallowed by implementation
        verify(broken, times(1)).sendSync(msg);
    }
}

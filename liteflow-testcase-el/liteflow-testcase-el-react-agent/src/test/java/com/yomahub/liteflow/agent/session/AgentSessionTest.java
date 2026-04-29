package com.yomahub.liteflow.agent.session;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AgentSessionTest {

    @Test
    void new_session_tracks_workspace_and_last_active_on_touch() throws InterruptedException {
        AgentSession s = new AgentSession("sid-1", Path.of("/tmp/sid-1"));
        Instant t0 = s.getLastActive();
        Thread.sleep(5);
        s.touch();
        assertTrue(s.getLastActive().isAfter(t0));
        assertEquals("sid-1", s.getSessionId());
        assertEquals(Path.of("/tmp/sid-1"), s.getWorkspaceDir());
        assertNull(s.getAgent());
    }

    @Test
    void lock_is_reentrant() {
        AgentSession s = new AgentSession("x", Path.of("/tmp/x"));
        s.getLock().lock();
        try {
            s.getLock().lock();
            s.getLock().unlock();
        } finally {
            s.getLock().unlock();
        }
        assertFalse(s.getLock().isLocked());
    }

    @Test
    void agent_setter_is_once() {
        AgentSession s = new AgentSession("x", Path.of("/tmp/x"));
        Object dummy = new Object();
        s.setAgent(dummy);
        assertSame(dummy, s.getAgent());
    }
}

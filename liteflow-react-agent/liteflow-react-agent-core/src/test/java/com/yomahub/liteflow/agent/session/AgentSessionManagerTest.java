package com.yomahub.liteflow.agent.session;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.SessionConfig;
import com.yomahub.liteflow.property.agent.WorkspaceConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AgentSessionManagerTest {

    @TempDir Path tmp;

    private AgentConfig config(Path root) {
        AgentConfig c = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setRoot(root.toString());
        c.setWorkspace(w);
        SessionConfig s = new SessionConfig();
        s.setIdleTimeout(Duration.ofMillis(100));
        s.setCleanupInterval(Duration.ofMillis(50));
        s.setMaxSessions(3);
        c.setSession(s);
        return c;
    }

    @Test
    void acquire_creates_workspace_and_reuses_same_session() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            assertTrue(Files.isDirectory(a.getWorkspaceDir()));
            AgentSession b = mgr.acquire("s1");
            assertSame(a, b, "same sessionId must reuse same AgentSession");
        } finally {
            mgr.close();
        }
    }

    @Test
    void safe_session_id_rejects_path_traversal() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession s = mgr.acquire("../../etc/passwd");
            assertTrue(s.getWorkspaceDir().startsWith(tmp),
                    "workspace must stay under root");
        } finally {
            mgr.close();
        }
    }

    @Test
    void expired_sessions_cleaned_up() throws Exception {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            Path ws = a.getWorkspaceDir();
            Thread.sleep(800);
            assertFalse(mgr.contains("s1"), "idle expired session should be cleaned");
            assertFalse(Files.exists(ws), "workspace directory should be deleted");
        } finally {
            mgr.close();
        }
    }

    @Test
    void active_locked_session_not_cleaned() throws Exception {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            AgentSession a = mgr.acquire("s1");
            a.getLock().lock();
            try {
                Thread.sleep(800);
                assertTrue(mgr.contains("s1"), "locked session should NOT be cleaned");
            } finally {
                a.getLock().unlock();
            }
        } finally {
            mgr.close();
        }
    }

    @Test
    void missing_root_raises_config_exception() {
        AgentConfig c = new AgentConfig();
        WorkspaceConfig w = new WorkspaceConfig();
        w.setRoot(null);
        c.setWorkspace(w);
        assertThrows(AgentConfigException.class, () -> new AgentSessionManager(c));
    }

    @Test
    void lru_evicts_oldest_when_exceeding_max() throws IOException {
        AgentSessionManager mgr = new AgentSessionManager(config(tmp));
        try {
            mgr.acquire("s1"); mgr.acquire("s2"); mgr.acquire("s3");
            mgr.acquire("s4"); // exceeds maxSessions=3
            assertFalse(mgr.contains("s1"));
            assertTrue(mgr.contains("s2"));
            assertTrue(mgr.contains("s3"));
            assertTrue(mgr.contains("s4"));
        } finally {
            mgr.close();
        }
    }
}

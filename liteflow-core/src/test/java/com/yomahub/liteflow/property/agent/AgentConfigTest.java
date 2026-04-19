package com.yomahub.liteflow.property.agent;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @Test
    void defaults_are_sensible() {
        AgentConfig c = new AgentConfig();
        assertNotNull(c.getWorkspace());
        assertNotNull(c.getSession());
        assertNotNull(c.getShell());
        assertNotNull(c.getDefaults());
        assertNotNull(c.getOpenaiCompatible());
        assertTrue(c.getOpenaiCompatible().isEmpty());

        WorkspaceConfig w = c.getWorkspace();
        assertNull(w.getRoot());
        assertTrue(w.isAutoCreate());
        assertTrue(w.isCleanupOnSessionExpire());
        assertFalse(w.isCleanupOnJvmShutdown());
        assertEquals(10 * 1024 * 1024, w.getMaxFileBytes());
        assertEquals(1000, w.getMaxListSize());

        SessionConfig s = c.getSession();
        assertEquals(Duration.ofMinutes(30), s.getIdleTimeout());
        assertEquals(Duration.ofMinutes(1), s.getCleanupInterval());
        assertEquals(10_000, s.getMaxSessions());

        ShellConfig sh = c.getShell();
        assertEquals(ShellMode.WHITELIST, sh.getMode());
        assertNotNull(sh.getWhitelist());
        assertNotNull(sh.getBlacklist());
        assertEquals(Duration.ofSeconds(30), sh.getTimeout());
        assertEquals(1024 * 1024, sh.getMaxOutputBytes());

        assertEquals(15, c.getDefaults().getMaxIterations());
    }

    @Test
    void platform_credentials_are_independent_instances() {
        AgentConfig c = new AgentConfig();
        c.getOpenai().setApiKey("k1");
        assertNull(c.getAnthropic().getApiKey());
        assertNull(c.getGemini().getApiKey());
        assertNull(c.getDashscope().getApiKey());
    }
}

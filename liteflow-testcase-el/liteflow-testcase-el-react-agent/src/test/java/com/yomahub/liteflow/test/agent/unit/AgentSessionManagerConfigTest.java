package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.session.AgentSession;
import com.yomahub.liteflow.agent.session.AgentSessionManager;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 覆盖 guide §2.1 / §6.2 中 workspace 配置和 AgentConfigException 抛出条件，
 * 以及 {@link AgentSessionManager#acquire(String, String)} 的 safeId / 缓存复用语义。
 */
public class AgentSessionManagerConfigTest {

    @TempDir
    Path tempBase;

    private static AgentConfig newConfigWithRoot(Path root) {
        AgentConfig cfg = new AgentConfig();
        cfg.getWorkspace().setRoot(root.toString());
        cfg.getSession().getMemory().setMode(MemoryStorageMode.NONE);
        return cfg;
    }

    @Test
    public void testNullWorkspaceRootThrowsAgentConfigException() {
        AgentConfig cfg = new AgentConfig();
        cfg.getSession().getMemory().setMode(MemoryStorageMode.NONE);
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> new AgentSessionManager(cfg));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.workspace.root is required"));
    }

    @Test
    public void testAutoCreateFalseAndMissingRootThrows() {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("does-not-exist"));
        cfg.getWorkspace().setAutoCreate(false);
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> new AgentSessionManager(cfg));
        Assertions.assertTrue(ex.getMessage().contains("workspace root does not exist"));
    }

    @Test
    public void testAutoCreateTrueCreatesRoot() {
        Path root = tempBase.resolve("autocreate-root");
        AgentConfig cfg = newConfigWithRoot(root);
        cfg.getWorkspace().setAutoCreate(true);
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            Assertions.assertTrue(Files.isDirectory(root));
            Assertions.assertNotNull(mgr);
        }
    }

    @Test
    public void testAcquireCreatesWorkspaceDirectoryAndCachesSession() throws IOException {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("ws"));
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            AgentSession s1 = mgr.acquire("conv-a", "agent-x");
            AgentSession s2 = mgr.acquire("conv-a", "agent-x");
            Assertions.assertSame(s1, s2, "同 (cid, key) 应复用同一 AgentSession");
            Assertions.assertTrue(Files.isDirectory(s1.getWorkspaceDir()));
        }
    }

    @Test
    public void testAcquireDifferentAgentKeysShareWorkspaceButGetDistinctSessions() {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("ws-multi"));
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            AgentSession a = mgr.acquire("conv-x", "agent-a");
            AgentSession b = mgr.acquire("conv-x", "agent-b");
            Assertions.assertNotSame(a, b);
            Assertions.assertEquals(a.getWorkspaceDir(), b.getWorkspaceDir(),
                    "同一 conversationId 下不同 agentKey 应共享 workspace 目录");
        }
    }

    @Test
    public void testUnsafeConversationIdGetsUrlEncodedDirectoryName() {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("ws-unsafe"));
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            AgentSession s = mgr.acquire("chat/user 1", "agent");
            String name = s.getWorkspaceDir().getFileName().toString();
            Assertions.assertNotEquals("chat/user 1", name);
            Assertions.assertFalse(name.contains("/"));
            Assertions.assertFalse(name.contains(" "));
            Assertions.assertFalse(name.contains("%"));
        }
    }

    @Test
    public void testEmptyConversationIdBecomesUnderscoreDirectory() {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("ws-empty"));
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            AgentSession s = mgr.acquire("", "agent");
            Assertions.assertEquals("_", s.getWorkspaceDir().getFileName().toString());
        }
    }

    @Test
    public void testContainsReflectsAcquiredSessions() {
        AgentConfig cfg = newConfigWithRoot(tempBase.resolve("ws-contains"));
        try (AgentSessionManager mgr = new AgentSessionManager(cfg)) {
            Assertions.assertFalse(mgr.contains("conv", "agent"));
            mgr.acquire("conv", "agent");
            Assertions.assertTrue(mgr.contains("conv", "agent"));
        }
    }
}

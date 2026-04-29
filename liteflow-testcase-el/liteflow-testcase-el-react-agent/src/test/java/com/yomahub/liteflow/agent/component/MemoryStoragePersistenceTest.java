package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link MemoryStorageMode#WORKSPACE} persists and restores
 * conversation history across simulated process restarts within the same JVM.
 */
class MemoryStoragePersistenceTest {

    @TempDir Path tmp;

    @AfterEach
    void cleanup() {
        ReActAgentComponent.AgentSessionManagerHolder.resetForTesting();
        LiteflowConfigGetter.clean();
    }

    private void primeConfig(MemoryStorageMode mode) {
        AgentConfig agent = new AgentConfig();
        agent.getWorkspace().setRoot(tmp.toString());
        agent.getWorkspace().setCleanupOnSessionExpire(false);
        agent.getShell().setMode(ShellMode.DISABLED);
        agent.getSession().getMemory().setMode(mode);
        LiteflowConfig cfg = new LiteflowConfig();
        cfg.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(cfg);
        DataBus.init();
    }

    static class TestAgent extends ReActAgentComponent {
        @Override
        @SuppressWarnings("rawtypes")
        protected ModelSpec model(ReActAgentContext ctx) {
            return new ModelSpec() {
                @Override public io.agentscope.core.model.Model resolve(AgentConfig cfg) {
                    return new FakeEchoModel();
                }
            };
        }
        @Override protected String systemPrompt(ReActAgentContext ctx) { return "you are helpful"; }
        @Override protected String userPrompt(ReActAgentContext ctx) {
            return (String) ctx.getSlot().getChainReqData(ctx.getSlot().getChainId());
        }
    }

    private void runOnce(String sid, String userInput) throws Exception {
        int slotIndex = DataBus.offerSlotByBean(Collections.emptyList());
        try {
            Slot slot = DataBus.getSlot(slotIndex);
            slot.putRequestId(sid);
            String chainId = "testChain";
            slot.setChainId(chainId);
            slot.setChainReqData(chainId, userInput);
            TestAgent agent = new TestAgent();
            agent.setNodeId("testAgent");
            Node node = new Node();
            node.setInstance(agent);
            node.setSlotIndex(slotIndex);
            node.setCurrChainId(chainId);
            agent.setRefNode(node);
            agent.process();
        } finally {
            DataBus.releaseSlot(slotIndex);
        }
    }

    @Test
    void workspace_mode_persists_messages_across_restart() throws Exception {
        primeConfig(MemoryStorageMode.WORKSPACE_FILE);

        runOnce("sid-A", "hello");
        runOnce("sid-A", "goodbye");

        // Persisted JSONL file should exist with at least the two user messages.
        Path persisted = tmp.resolve(".agent-session").resolve("sid-A").resolve("memory_messages.jsonl");
        assertTrue(Files.isRegularFile(persisted), "persisted memory file should exist");
        List<String> lines = Files.readAllLines(persisted);
        long userLines = lines.stream().filter(l -> l.contains("\"hello\"") || l.contains("\"goodbye\"")).count();
        assertEquals(2, userLines, "both user prompts must be persisted");

        // Simulate a JVM restart by resetting the manager singleton.
        ReActAgentComponent.AgentSessionManagerHolder.resetForTesting();

        // Acquire again with the same sid; loadIfExists should restore messages
        // before the next call appends new ones.
        runOnce("sid-A", "third");
        List<String> after = Files.readAllLines(persisted);
        assertTrue(after.size() >= lines.size() + 1,
                "after restart, new messages should append on top of restored history (was "
                        + lines.size() + ", now " + after.size() + ")");
    }

    @Test
    void none_mode_skips_persistence() throws Exception {
        primeConfig(MemoryStorageMode.NONE);
        runOnce("sid-B", "hi");
        Path persistedDir = tmp.resolve(".agent-session");
        assertFalse(Files.exists(persistedDir),
                "NONE mode must not create the persistence sub-directory");
    }
}

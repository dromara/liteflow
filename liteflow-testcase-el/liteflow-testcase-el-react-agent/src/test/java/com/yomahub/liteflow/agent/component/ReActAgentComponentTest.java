package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ReActAgentComponentTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void init() {
        AgentConfig agent = new AgentConfig();
        agent.getWorkspace().setRoot(tmp.toString());
        agent.getShell().setMode(ShellMode.DISABLED);
        LiteflowConfig cfg = new LiteflowConfig();
        cfg.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(cfg);
        DataBus.init();
    }

    @AfterEach
    void cleanup() {
        ReActAgentComponent.AgentSessionManagerHolder.resetForTesting();
        LiteflowConfigGetter.clean();
    }

    /**
     * Concrete subclass under test.
     */
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
        @Override
        protected String systemPrompt(ReActAgentContext ctx) { return "you are helpful"; }
        @Override
        protected String userPrompt(ReActAgentContext ctx) {
            return (String) ctx.getSlot().getChainReqData(ctx.getSlot().getChainId());
        }
    }

    /**
     * End-to-end test: process() builds an agent, calls it, and writes the reply
     * into the slot's response data.  Also verifies that the session workspace
     * directory is created.
     */
    @Test
    void process_creates_workspace_and_writes_response() throws Exception {
        // 1. Create a slot via DataBus
        int slotIndex = DataBus.offerSlotByBean(Collections.emptyList());
        Slot slot = DataBus.getSlot(slotIndex);
        slot.putRequestId("req-1");
        String chainId = "testChain";
        slot.setChainId(chainId);
        slot.setChainReqData(chainId, "hello");

        // 2. Wire the NodeComponent into a Node with the slot index
        TestAgent agent = new TestAgent();
        agent.setNodeId("testAgent");
        Node node = new Node();
        node.setInstance(agent);
        node.setSlotIndex(slotIndex);
        node.setCurrChainId(chainId);
        agent.setRefNode(node);

        // 3. Execute
        agent.process();

        // 4. Verify workspace was created
        assertTrue(Files.isDirectory(tmp.resolve("req-1")),
                "session workspace directory should exist under the temp root");

        // 5. Verify response data
        assertEquals("[echo]", slot.getResponseData(),
                "response data should be the fake echo text");

        // 6. Cleanup
        DataBus.releaseSlot(slotIndex);
    }
}

package com.yomahub.liteflow.agent.testsupport;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;

import java.time.Instant;

/** Deterministic invocation context fixtures shared by middleware tests. */
public final class AgentTestContexts {

    private AgentTestContexts() {
    }

    public static LiteFlowAgentContext liteFlowContext() {
        Slot slot = new Slot();
        slot.setChainId("chain-1");
        slot.setConversationId("conversation-1");
        slot.putRequestId("request-1");
        return new LiteFlowAgentContext(
                new AgentInvocationIdentity(
                        "namespace-1",
                        "conversation-1",
                        "agent-1",
                        null,
                        null,
                        null),
                slot,
                "chain-1",
                "node-1",
                "request-1",
                "trace-1",
                Instant.parse("2030-01-01T00:00:00Z"),
                AgentOutputSpec.text(),
                LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + "test");
    }

    public static RuntimeContext runtimeContext(LiteFlowAgentContext context) {
        return RuntimeContext.builder()
                .userId(null)
                .sessionId(context.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, context)
                .put(Slot.class, context.getSlot())
                .build();
    }
}

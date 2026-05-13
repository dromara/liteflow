package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component("parallelAgentB")
public class ParallelAgentBCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_REPLY = new AtomicReference<>();

    public static void reset() {
        SEEN_AGENT_KEY.set(null);
        SEEN_REPLY.set(null);
    }

    @Override
    protected String agentKey() {
        return "parallelAgentB__" + getSlot().getRequestId();
    }

    @Override
    protected String userPrompt() {
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        return super.userPrompt();
    }

    @Override
    protected void handleReply(Msg reply) {
        String text = reply == null ? "" : reply.getTextContent();
        SEEN_REPLY.set(text);
        ctx().getSlot().setOutput("parallelAgentB", text);
    }
}

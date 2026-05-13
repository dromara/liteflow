package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * WHEN 并发分支 Agent A：覆写 agentKey，确保和分支 B 拥有独立的 Session 锁，
 * 这样才能真正并发执行而不互相串行。
 */
@Component("parallelAgentA")
public class ParallelAgentACmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_REPLY = new AtomicReference<>();

    public static void reset() {
        SEEN_AGENT_KEY.set(null);
        SEEN_REPLY.set(null);
    }

    @Override
    protected String agentKey() {
        return "parallelAgentA__" + getSlot().getRequestId();
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
        ctx().getSlot().setOutput("parallelAgentA", text);
    }
}

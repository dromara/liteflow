package com.yomahub.liteflow.test.agent.feature.runtimecontext;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component("runtimeContextAgent")
public final class RuntimeContextAgentCmp extends ReActAgentComponent {

    private static final AtomicInteger RUNTIME_BUILDS = new AtomicInteger();
    private static volatile ScriptedChatModel model = newModel();

    static void reset() {
        RUNTIME_BUILDS.set(0);
        model = newModel();
    }

    static int runtimeBuilds() {
        return RUNTIME_BUILDS.get();
    }

    static ScriptedChatModel scriptedModel() {
        return model;
    }

    // 本场景断言固定 conversationId 的精确消息数，用进程内状态存储避免跨运行持久化干扰。
    @Override
    protected com.yomahub.liteflow.agent.state.AgentStateStoreResolver stateStoreResolver() {
        return config -> new com.yomahub.liteflow.agent.state.ResolvedAgentStateStore(
                new io.agentscope.core.state.InMemoryAgentStateStore(), true);
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel override must be used");
    }

    @Override
    protected Model buildModel() {
        RUNTIME_BUILDS.incrementAndGet();
        return model;
    }

    @Override
    protected String systemPrompt() {
        return "Reply deterministically.";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        Object request = getSlot().getChainReqData(getSlot().getChainId());
        return request == null ? "" : request.toString();
    }

    private static ScriptedChatModel newModel() {
        return ScriptedChatModel.builder()
                .reply("reply-a1")
                .reply("reply-b1")
                .reply("reply-a2")
                .build();
    }
}

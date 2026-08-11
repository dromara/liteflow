package com.yomahub.liteflow.test.agent.feature.sessionreuse;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.middleware.MiddlewareBase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 会话 / Session 复用 Agent。固定 conversationId 让多次调用进入同一 Session，
 * AgentProbe 捕获 agentId 以断言 ReActAgent 实例是否复用。
 *
 * <p>探针通过 AgentScope 2 middleware 注册到组件持有的 runtime。
 */
@Component("memoryAgent")
public class MemoryAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "sessionreuse-conversation";
    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected String resolveConversationId(com.yomahub.liteflow.slot.Slot slot) {
        return FIXED_CONVERSATION_ID;
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.middleware());
    }
}

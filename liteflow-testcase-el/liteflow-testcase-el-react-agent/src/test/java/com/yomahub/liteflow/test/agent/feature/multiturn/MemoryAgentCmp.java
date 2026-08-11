package com.yomahub.liteflow.test.agent.feature.multiturn;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import com.yomahub.liteflow.test.agent.support.ForwardingProbeMiddleware;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 会话 / Session 复用 Agent。固定 conversationId 让多次调用进入同一 Session，
 * AgentProbe 捕获 agentId 以断言 ReActAgent 实例是否复用。
 *
 * <p>探针通过 AgentScope 2 middleware 注册到组件持有的 runtime。
 */
@Component("memoryAgent")
public class MemoryAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "multiturn-conversation";
    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    private static final List<Integer> MODEL_MESSAGE_COUNTS = new CopyOnWriteArrayList<>();
    private static final MiddlewareBase FORWARDING_PROBE = new ForwardingProbeMiddleware(() -> {
        AgentProbe probe = PROBE.get();
        return probe == null ? null : probe.middleware();
    });

    public static void reset() {
        PROBE.set(new AgentProbe());
    }

    public static void resetModelObservations() {
        MODEL_MESSAGE_COUNTS.clear();
    }

    public static List<Integer> modelMessageCounts() {
        return List.copyOf(MODEL_MESSAGE_COUNTS);
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("deterministic buildModel override must bypass model spec");
    }

    @Override
    protected Model buildModel() {
        return new DeterministicHistoryModel("multi-turn-model", MODEL_MESSAGE_COUNTS);
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
        return List.of(FORWARDING_PROBE);
    }
}

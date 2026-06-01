package com.yomahub.liteflow.test.agent.feature.multiturn;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 会话 / Session 复用 Agent。固定 conversationId 让多次调用进入同一 Session，
 * AgentProbe 捕获 agentId 以断言 ReActAgent 实例是否复用。
 *
 * <p>由于 hooks() 只在首次构建缓存 Agent 时求值，这里注册一个固定的转发 Hook，
 * 运行期再委派到当前 PROBE，使得每次 reset() 后仍能抓到本轮事件。
 */
@Component("memoryAgent")
public class MemoryAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "multiturn-conversation";
    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    private static final Hook PROBE_FORWARDING_HOOK = new Hook() {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            AgentProbe probe = PROBE.get();
            return probe == null ? Mono.just(event) : probe.hook().onEvent(event);
        }
    };

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
    protected String userPrompt() {
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
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected String resolveConversationId() {
        return FIXED_CONVERSATION_ID;
    }

    @Override
    protected List<Hook> hooks() {
        return List.of(PROBE_FORWARDING_HOOK);
    }
}

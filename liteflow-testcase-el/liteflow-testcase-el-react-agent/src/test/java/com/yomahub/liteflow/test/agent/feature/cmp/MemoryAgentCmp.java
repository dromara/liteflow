package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import com.yomahub.liteflow.test.agent.support.FakeEchoModel;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 记忆 / Session 复用 Agent。固定 conversationId 让两次调用进入同一 Session，
 * AgentProbe 捕获 agentId 以断言 ReActAgent 实例是否复用。
 */
@Component("memoryAgent")
public class MemoryAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final String FIXED_CONVERSATION_ID = "memory-test-conversation";
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
    protected String resolveConversationId() {
        return FIXED_CONVERSATION_ID;
    }

    @Override
    protected Model buildModel() {
        return new FakeEchoModel("fake-memory-model");
    }

    @Override
    protected List<Hook> hooks() {
        return List.of(PROBE_FORWARDING_HOOK);
    }
}

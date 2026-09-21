package com.yomahub.liteflow.test.agent.feature.hitl;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.guard.LocalAgentInvocationGuard;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.ScriptedChatModel;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import reactor.core.publisher.Flux;

@Component("hitlAgent")
public final class HitlAgentCmp extends HarnessAgentComponent {

    private static final HitlRecordingGuard GUARD = new HitlRecordingGuard();
    private static final List<Integer> ACTIVE = new CopyOnWriteArrayList<>();
    private static final List<List<io.agentscope.core.message.Msg>> AGENT_INPUTS =
            new CopyOnWriteArrayList<>();
    private static volatile ScriptedChatModel model = newModel();

    static void reset() {
        GUARD.reset();
        ACTIVE.clear();
        AGENT_INPUTS.clear();
        model = newModel();
    }

    static HitlRecordingGuard guard() { return GUARD; }
    static ScriptedChatModel scriptedModel() { return model; }
    static List<Integer> activeLeaseObservations() { return List.copyOf(ACTIVE); }
    static List<io.agentscope.core.message.Msg> continuationInput() {
        return AGENT_INPUTS.get(1);
    }

    // 本场景断言固定 conversationId 的续轮事务，用进程内状态存储避免跨运行持久化干扰。
    @Override
    protected com.yomahub.liteflow.agent.state.AgentStateStoreResolver stateStoreResolver() {
        return config -> new com.yomahub.liteflow.agent.state.ResolvedAgentStateStore(
                new io.agentscope.core.state.InMemoryAgentStateStore(), true);
    }

    @Override protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel override must be used");
    }

    @Override protected Model buildModel() { return model; }
    @Override protected String systemPrompt() { return "Request approval once."; }
    @Override protected String userPrompt(LiteFlowAgentContext context) { return "approve"; }
    @Override protected List<Object> tools() { return List.of(new ApprovalTool()); }
    @Override protected List<MiddlewareBase> middlewares() {
        return List.of(new MiddlewareBase() {
            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent, RuntimeContext context, AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                AGENT_INPUTS.add(List.copyOf(input.msgs()));
                return next.apply(input);
            }
        });
    }

    @Override
    protected PermissionContextState permissionContext() {
        return PermissionContextState.builder()
                .addAskRule("approval_probe", new PermissionRule(
                        "approval_probe", null, PermissionBehavior.ASK, "offline test"))
                .build();
    }

    @Override
    protected AgentConfirmationHandler confirmationHandler() {
        return (event, context) -> {
            ACTIVE.add(GUARD.active());
            return Mono.just(event.getToolCalls().stream()
                    .map(tool -> new ConfirmResult(true, tool)).toList());
        };
    }

    private static ScriptedChatModel newModel() {
        ToolUseBlock tool = new ToolUseBlock(
                "approval-1", "approval_probe", Map.of("value", "ok"),
                "{\"value\":\"ok\"}", Map.of(), ToolCallState.PENDING);
        return ScriptedChatModel.builder()
                .observeCalls(ignored -> ACTIVE.add(GUARD.active()))
                .tool(tool)
                .reply("approved")
                .build();
    }

    static final class ApprovalTool {
        @Tool(name = "approval_probe", description = "Record an approved value")
        public String approve(@ToolParam(name = "value", description = "value") String value) {
            return "approved:" + value;
        }
    }
}

final class HitlRecordingGuard implements AgentInvocationGuard {
    private final LocalAgentInvocationGuard delegate = new LocalAgentInvocationGuard();
    private final AtomicInteger active = new AtomicInteger();

    @Override
    public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
        AgentInvocationLease lease = delegate.acquire(key, timeout);
        active.incrementAndGet();
        AtomicBoolean closed = new AtomicBoolean();
        return new AgentInvocationLease() {
            @Override public AgentInvocationKey key() { return lease.key(); }
            @Override public void close() {
                if (closed.compareAndSet(false, true)) {
                    lease.close();
                    active.decrementAndGet();
                }
            }
        };
    }

    int active() { return active.get(); }
    void reset() {
        if (active.get() != 0) throw new IllegalStateException("guard still active");
    }
}

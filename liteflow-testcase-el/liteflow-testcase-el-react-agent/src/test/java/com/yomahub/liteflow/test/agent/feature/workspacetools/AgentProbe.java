package com.yomahub.liteflow.test.agent.feature.workspacetools;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** AgentScope 2 middleware probe used by the live integration tests. */
public final class AgentProbe {

    private final AtomicInteger reasoningEventCount = new AtomicInteger();
    private final AtomicInteger actingEventCount = new AtomicInteger();
    private final AtomicReference<Integer> maxIters = new AtomicReference<>();
    private final AtomicReference<String> agentId = new AtomicReference<>();
    private final AtomicReference<Set<String>> toolNamesSnapshot = new AtomicReference<>();
    private final Set<String> calledToolNames = ConcurrentHashMap.newKeySet();

    public MiddlewareBase middleware() {
        return new MiddlewareBase() {
            @Override
            public Flux<AgentEvent> onReasoning(
                    Agent agent,
                    RuntimeContext context,
                    ReasoningInput input,
                    Function<ReasoningInput, Flux<AgentEvent>> next) {
                reasoningEventCount.incrementAndGet();
                capture(agent);
                return next.apply(input);
            }

            @Override
            public Flux<AgentEvent> onActing(
                    Agent agent,
                    RuntimeContext context,
                    ActingInput input,
                    Function<ActingInput, Flux<AgentEvent>> next) {
                actingEventCount.incrementAndGet();
                capture(agent);
                if (input != null && input.toolCalls() != null) {
                    input.toolCalls().forEach(tool -> calledToolNames.add(tool.getName()));
                }
                return next.apply(input);
            }
        };
    }

    private void capture(Agent agent) {
        if (agent == null) {
            return;
        }
        agentId.compareAndSet(null, agent.getAgentId());
        if (agent instanceof ReActAgent reactAgent) {
            maxIters.compareAndSet(null, reactAgent.getMaxIters());
            toolNamesSnapshot.compareAndSet(
                    null, new TreeSet<>(reactAgent.getToolkit().getToolNames()));
        }
    }

    public int reasoningCount() {
        return reasoningEventCount.get();
    }

    public int actingCount() {
        return actingEventCount.get();
    }

    public Integer observedMaxIters() {
        return maxIters.get();
    }

    public String observedAgentId() {
        return agentId.get();
    }

    public Set<String> toolNames() {
        Set<String> snapshot = toolNamesSnapshot.get();
        return snapshot == null ? Set.of() : Set.copyOf(snapshot);
    }

    public Set<String> calledTools() {
        return Set.copyOf(calledToolNames);
    }
}

package com.yomahub.liteflow.test.agent.feature.workspacetools;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通过 agentscope Hook 抓取 ReActAgent 运行过程的可观察信号，
 * 让测试在不 mock 模型的前提下做白盒断言（工具集、迭代上限、agentId、工具调用名）。
 *
 * <p>按模块隔离约定，每个 package 各自冗余一份该探针。
 */
public final class AgentProbe {

    private final AtomicInteger reasoningEventCount = new AtomicInteger();
    private final AtomicInteger actingEventCount = new AtomicInteger();
    private final AtomicReference<Integer> maxIters = new AtomicReference<>();
    private final AtomicReference<String> agentId = new AtomicReference<>();
    private final AtomicReference<Set<String>> toolNamesSnapshot = new AtomicReference<>();
    private final Set<String> calledToolNames = ConcurrentHashMap.newKeySet();

    public Hook hook() {
        return new Hook() {
            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                if (event instanceof PreReasoningEvent) {
                    reasoningEventCount.incrementAndGet();
                    if (event.getAgent() instanceof ReActAgent agent) {
                        maxIters.compareAndSet(null, agent.getMaxIters());
                        agentId.compareAndSet(null, agent.getAgentId());
                        toolNamesSnapshot.compareAndSet(null,
                                new TreeSet<>(agent.getToolkit().getToolNames()));
                    }
                } else if (event instanceof PreActingEvent acting) {
                    actingEventCount.incrementAndGet();
                    if (acting.getToolUse() != null) {
                        calledToolNames.add(acting.getToolUse().getName());
                    }
                } else if (event instanceof PostActingEvent post) {
                    if (post.getToolUse() != null) {
                        calledToolNames.add(post.getToolUse().getName());
                    }
                }
                return Mono.just(event);
            }
        };
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
        Set<String> snap = toolNamesSnapshot.get();
        return snap == null ? Set.of() : Set.copyOf(snap);
    }

    public Set<String> calledTools() {
        return Set.copyOf(calledToolNames);
    }
}

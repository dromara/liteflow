package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.ChatUsage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatUsageMiddlewareTest {

    @Test
    void aggregatesAllModelCallsOnceAndKeepsInvocationsIsolated() {
        ChatUsageMiddleware middleware = new ChatUsageMiddleware();
        LiteFlowAgentContext first = AgentTestContexts.liteFlowContext();
        LiteFlowAgentContext second = AgentTestContexts.liteFlowContext();
        ModelCallInput input = new ModelCallInput(List.of(), List.of(), null, null);
        ModelCallEndEvent firstCall = new ModelCallEndEvent(
                "reply-1", new ChatUsage(2, 3, 1, 0.5));
        ModelCallEndEvent secondCall = new ModelCallEndEvent(
                "reply-2", new ChatUsage(5, 7, 4, 1.25));

        Flux.merge(
                        middleware.onModelCall(
                                        null,
                                        AgentTestContexts.runtimeContext(first),
                                        input,
                                        ignored -> Flux.just(firstCall, firstCall))
                                .subscribeOn(Schedulers.parallel()),
                        middleware.onModelCall(
                                        null,
                                        AgentTestContexts.runtimeContext(first),
                                        input,
                                        ignored -> Flux.just(secondCall))
                                .subscribeOn(Schedulers.parallel()))
                .blockLast();

        ChatUsage usage = first.getChatUsage();
        assertEquals(7, usage.getInputTokens());
        assertEquals(10, usage.getOutputTokens());
        assertEquals(5, usage.getCachedTokens());
        assertEquals(1.75, usage.getTime(), 0.0001);
        assertNull(second.getChatUsage());
    }
}

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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void saturatesMalformedAndOverflowingUsageUnderConcurrentAggregation() {
        LiteFlowAgentContext malformedFirst = AgentTestContexts.liteFlowContext();
        malformedFirst.recordChatUsage(
                "malformed-first",
                new ChatUsage(-1, -2, -3, Double.NaN));

        ChatUsage sanitized = malformedFirst.getChatUsage();
        assertEquals(0, sanitized.getInputTokens());
        assertEquals(0, sanitized.getOutputTokens());
        assertEquals(0, sanitized.getCachedTokens());
        assertEquals(0.0, sanitized.getTime());

        LiteFlowAgentContext concurrent = AgentTestContexts.liteFlowContext();
        List<ChatUsage> contributions = IntStream.range(0, 128)
                .mapToObj(index -> switch (index) {
                    case 0 -> new ChatUsage(
                            Integer.MAX_VALUE,
                            -7,
                            Integer.MAX_VALUE,
                            Double.MAX_VALUE);
                    case 1 -> new ChatUsage(1, 1, 1, Double.POSITIVE_INFINITY);
                    case 2 -> new ChatUsage(-100, -100, -100, Double.NaN);
                    default -> new ChatUsage(1, 1, 1, 0.25);
                })
                .toList();

        Flux.range(0, contributions.size())
                .parallel()
                .runOn(Schedulers.parallel())
                .doOnNext(index -> concurrent.recordChatUsage(
                        "boundary-" + index,
                        contributions.get(index)))
                .sequential()
                .blockLast();

        ChatUsage usage = concurrent.getChatUsage();
        assertEquals(Integer.MAX_VALUE, usage.getInputTokens());
        assertEquals(126, usage.getOutputTokens());
        assertEquals(Integer.MAX_VALUE, usage.getCachedTokens());
        assertEquals(Integer.MAX_VALUE, usage.getTotalTokens());
        assertEquals(Double.MAX_VALUE, usage.getTime());
        assertFalse(Double.isNaN(usage.getTime()));
        assertFalse(Double.isInfinite(usage.getTime()));
    }
}

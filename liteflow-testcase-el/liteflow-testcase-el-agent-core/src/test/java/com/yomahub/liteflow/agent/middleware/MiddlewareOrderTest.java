package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddlewareOrderTest {

    @Test
    void builtInAndUserMiddlewareExposeTheDocumentedOnionPriorities() {
        MiddlewareBase delegate = new CountingMiddleware();
        MiddlewareBase user = AgentMiddlewareOrder.user(delegate);

        assertEquals(10_000, AgentMiddlewareOrder.STATE_STORE_FAILURE);
        assertEquals(9_000, new AgentLoggingMiddleware(false).order());
        assertEquals(8_000,
                new FlowEventBridgeMiddleware(AgentListenerFailureMode.FAIL_FAST).order());
        assertEquals(7_000, new ChatUsageMiddleware().order());
        assertEquals(7_000, new SkillTrackingMiddleware(Map.of()).order());
        assertEquals(1_000, user.order());
        assertEquals(37, delegate.order(), "normalization must not mutate caller middleware");
    }

    @Test
    void normalizedUserMiddlewareDelegatesInputsAndDoesNotHideErrors() {
        RuntimeException failure = new RuntimeException("user middleware failure");
        AtomicInteger nextCalls = new AtomicInteger();
        MiddlewareBase delegate = new MiddlewareBase() {
            @Override
            public Flux<AgentEvent> onAgent(
                    Agent agent,
                    RuntimeContext context,
                    AgentInput input,
                    Function<AgentInput, Flux<AgentEvent>> next) {
                assertSame(input, nextInput(input, next, nextCalls));
                return Flux.error(failure);
            }
        };
        MiddlewareBase normalized = AgentMiddlewareOrder.user(delegate);

        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> normalized.onAgent(
                                null,
                                RuntimeContext.empty(),
                                new AgentInput(List.of()),
                                ignored -> Flux.empty())
                        .blockLast());

        assertSame(failure, thrown);
        assertEquals(1, nextCalls.get());
    }

    @Test
    void publicInspectionRecursivelyExposesUserWrapperLayersAsReadOnly() {
        MiddlewareBase delegate = new CountingMiddleware();
        MiddlewareBase inner = AgentMiddlewareOrder.user(delegate);
        MiddlewareBase outer = AgentMiddlewareOrder.user(inner);

        List<MiddlewareBase> layers = AgentMiddlewareOrder.inspect(outer);

        assertEquals(List.of(outer, inner, delegate), layers);
        assertThrows(UnsupportedOperationException.class,
                () -> layers.add(new CountingMiddleware()));
        assertEquals(AgentMiddlewareOrder.USER, outer.order());
        assertEquals(AgentMiddlewareOrder.USER, inner.order());
        assertEquals(37, delegate.order());
    }

    @Test
    void publicSystemPromptMiddlewareUsesOnlyTheCurrentRuntimeContext() {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        AtomicInteger calls = new AtomicInteger();
        LiteFlowSystemPromptMiddleware middleware = new LiteFlowSystemPromptMiddleware(
                (prompt, context) -> {
                    calls.incrementAndGet();
                    assertSame(invocation, context);
                    return Mono.just(prompt + " / " + context.getRequestId());
                });

        String transformed = middleware.onSystemPrompt(
                        null,
                        AgentTestContexts.runtimeContext(invocation),
                        "base")
                .block();

        assertEquals("base / request-1", transformed);
        assertEquals(1, calls.get());
        AgentConfigException missingContext = assertThrows(
                AgentConfigException.class,
                () -> middleware.onSystemPrompt(null, RuntimeContext.empty(), "base").block());
        assertTrue(missingContext.getMessage().contains("LiteFlowAgentContext"));
        assertEquals(1, calls.get());
    }

    @Test
    void skillTrackingRecordsOnlySuccessfulRealLoadSkillResults() {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        SkillTrackingMiddleware middleware = new SkillTrackingMiddleware(
                Map.of("skill-id", "literal-skill-name"));
        ToolUseBlock load = new ToolUseBlock(
                "load-1",
                "load_skill_through_path",
                Map.of("skillId", "skill-id", "path", "SKILL.md"));
        ToolUseBlock failedLoad = new ToolUseBlock(
                "load-2",
                "load_skill_through_path",
                Map.of("skillId", "failed-id", "path", "SKILL.md"));

        middleware.onActing(
                        null,
                        AgentTestContexts.runtimeContext(invocation),
                        new ActingInput(List.of(load, failedLoad)),
                        ignored -> Flux.just(
                                new ToolResultEndEvent(
                                        "reply-1",
                                        "load-2",
                                        "load_skill_through_path",
                                        ToolResultState.ERROR),
                                new ToolResultEndEvent(
                                        "reply-1",
                                        "load-1",
                                        "load_skill_through_path",
                                        ToolResultState.SUCCESS)))
                .blockLast();

        assertEquals(List.of("literal-skill-name"), invocation.getUsedSkills());
    }

    @Test
    void loggingUsesCurrentSafeIdentifiersAndNeverReplacesExecutionFailure() {
        LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
        String secret = "sentinel-provider-api-key-and-tool-arguments";
        RuntimeException failure = new RuntimeException(secret);
        List<String> warnings = new ArrayList<>();
        AgentLoggingMiddleware middleware = new AgentLoggingMiddleware(true, warnings::add);

        assertEquals(
                "conversation=conversation-1 agent=agent-1 "
                        + "chain=chain-1 node=node-1 request=request-1",
                AgentLoggingMiddleware.contextLabel(invocation));
        assertEquals("😀...(truncated)", AgentLoggingMiddleware.truncate("😀😀", 1));
        assertFalse(Character.isHighSurrogate(
                AgentLoggingMiddleware.truncate("😀😀", 1).charAt(1)));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> middleware.onAgent(
                                null,
                                AgentTestContexts.runtimeContext(invocation),
                                new AgentInput(List.of()),
                                ignored -> Flux.error(failure))
                        .blockLast());
        assertSame(failure, thrown);
        assertEquals(1, warnings.size());
        assertFalse(warnings.get(0).contains(secret));
        assertTrue(warnings.get(0).contains("category=java.lang.RuntimeException"));
        assertTrue(warnings.get(0).contains("phase=agent"));
        assertTrue(warnings.get(0).contains("request=request-1"));
    }

    private static AgentInput nextInput(
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next,
            AtomicInteger nextCalls) {
        nextCalls.incrementAndGet();
        next.apply(input);
        return input;
    }

    private static final class CountingMiddleware implements MiddlewareBase {
        @Override
        public int order() {
            return 37;
        }
    }
}

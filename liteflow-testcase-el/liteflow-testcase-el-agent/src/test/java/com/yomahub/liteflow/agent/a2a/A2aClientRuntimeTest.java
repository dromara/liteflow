package com.yomahub.liteflow.agent.a2a;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class A2aClientRuntimeTest {

    @Test
    void createsOneIsolatedAgentForEverySubscription() {
        RecordingAgentFactory agents = new RecordingAgentFactory();
        RuntimeBinding binding = binding(agents);
        A2aClientRequest request = request("conversation-7", binding);

        Mono<Msg> call = binding.runtime.call(request);

        assertEquals(0, agents.created.get());
        StepVerifier.create(call).expectNextMatches(reply -> "reply-1".equals(reply.getTextContent()))
                .verifyComplete();
        StepVerifier.create(call).expectNextMatches(reply -> "reply-2".equals(reply.getTextContent()))
                .verifyComplete();
        assertEquals(2, agents.created.get());
        assertNotSame(agents.first, agents.second);
    }

    @Test
    void usesTheRuntimeOwnedResolverAndConfigForEveryAgent() {
        RecordingAgentFactory agents = new RecordingAgentFactory();
        AgentCardResolver ownedResolver = name -> null;
        A2aAgentConfig ownedConfig = A2aAgentConfig.builder().build();
        A2aClientRuntime runtime = A2aClientRuntimeFactories.perCall(agents)
                .create(ownedResolver, ownedConfig);
        RuntimeBinding binding = new RuntimeBinding(ownedResolver, ownedConfig, runtime);
        A2aClientRequest supplied = request("conversation-7", binding);

        runtime.call(supplied).block();

        assertSame(ownedResolver, agents.request.resolver());
        assertSame(ownedConfig, agents.request.config());
    }

    @Test
    void rejectsARequestWhoseResolverOrConfigDiffersFromTheRuntimeBinding() {
        RecordingAgentFactory agents = new RecordingAgentFactory();
        AgentCardResolver ownedResolver = name -> null;
        A2aAgentConfig ownedConfig = A2aAgentConfig.builder().build();
        A2aClientRuntime runtime = A2aClientRuntimeFactories.perCall(agents)
                .create(ownedResolver, ownedConfig);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> runtime.call(request("mismatch", binding(agents))).block());

        assertEquals("A2A request resolver/config must match its runtime binding",
                failure.getMessage());
        assertEquals(0, agents.created.get());
    }

    @Test
    void successDoesNotInterruptTheSubscribedAgent() {
        AtomicInteger interrupts = new AtomicInteger();
        RuntimeBinding binding = binding(
                Mono.just(new UserMessage("done")), interrupts, false);

        StepVerifier.create(binding.runtime.call(request("success", binding)))
                .expectNextMatches(reply -> "done".equals(reply.getTextContent()))
                .verifyComplete();

        assertEquals(0, interrupts.get());
    }

    @Test
    void remoteErrorDoesNotInterruptTheTerminalAgent() {
        RuntimeException remote = new RuntimeException("remote failed");
        AtomicInteger interrupts = new AtomicInteger();
        RuntimeBinding binding = binding(Mono.error(remote), interrupts, false);

        StepVerifier.create(binding.runtime.call(request("error", binding)))
                .expectErrorMatches(failure -> failure == remote)
                .verify();

        assertEquals(0, interrupts.get());
    }

    @Test
    void cancellationInterruptsOnlyItsSubscribedAgentOnce() {
        AtomicInteger interrupts = new AtomicInteger();
        RuntimeBinding binding = binding(Mono.never(), interrupts, false);

        StepVerifier.create(binding.runtime.call(request("cancel", binding)))
                .thenCancel()
                .verify();

        assertEquals(1, interrupts.get());
    }

    @Test
    void timeoutInterruptsOnceAndInterruptFailureDoesNotReplaceTimeout() {
        AtomicInteger interrupts = new AtomicInteger();
        RuntimeBinding binding = binding(Mono.never(), interrupts, true);

        StepVerifier.withVirtualTime(() -> binding.runtime.call(
                                request("timeout", Duration.ofSeconds(3), binding))
                        .timeout(Duration.ofSeconds(3)))
                .thenAwait(Duration.ofSeconds(3))
                .expectError(TimeoutException.class)
                .verify();

        assertEquals(1, interrupts.get());
    }

    @Test
    void distinctConversationsRunInParallelButEachAgentHasAtMostOneCall() {
        ConcurrentAgentFactory agents = new ConcurrentAgentFactory();
        RuntimeBinding binding = binding(agents);

        var replies = Mono.zip(
                        binding.runtime.call(request("conversation-a", binding)),
                        binding.runtime.call(request("conversation-b", binding)))
                .block(Duration.ofSeconds(1));

        assertEquals("reply-1", replies.getT1().getTextContent());
        assertEquals("reply-2", replies.getT2().getTextContent());
        assertEquals(2, agents.created.get());
        assertEquals(2, agents.maxGlobalActive.get());
        assertEquals(1, agents.maxPerAgentActive.get());
    }

    private static A2aClientRequest request(
            String conversationId, RuntimeBinding binding) {
        return request(conversationId, Duration.ofSeconds(1), binding);
    }

    private static A2aClientRequest request(
            String conversationId, Duration timeout, RuntimeBinding binding) {
        Slot slot = new Slot();
        AgentInvocationIdentity identity = new AgentInvocationIdentity(
                "a2a-test", conversationId, "node-a", null, null, null);
        LiteFlowAgentContext context = new LiteFlowAgentContext(
                identity, slot, "chain", "node-a", "request-9", "trace-4",
                Instant.now().plusSeconds(5), AgentOutputSpec.text(), "attachment");
        UserMessage message = UserMessage.builder()
                .textContent("hello")
                .metadata(Map.of(
                        "liteflow.conversationId", conversationId,
                        "liteflow.agentKey", "node-a",
                        "liteflow.traceId", "trace-4"))
                .build();
        return new A2aClientRequest(
                "remote", binding.resolver, binding.config, message,
                timeout, context);
    }

    private static RuntimeBinding binding(
            Mono<Msg> invocation, AtomicInteger interrupts, boolean failInterrupt) {
        A2aAgentFactory factory = request -> new A2aAgentHandle() {
            @Override
            public Mono<Msg> call(UserMessage message) {
                return invocation;
            }

            @Override
            public void interrupt() {
                interrupts.incrementAndGet();
                if (failInterrupt) {
                    throw new IllegalStateException("interrupt failed");
                }
            }
        };
        return binding(factory);
    }

    private static RuntimeBinding binding(A2aAgentFactory factory) {
        AgentCardResolver resolver = name -> null;
        A2aAgentConfig config = A2aAgentConfig.builder().build();
        A2aClientRuntime runtime = A2aClientRuntimeFactories.perCall(factory)
                .create(resolver, config);
        return new RuntimeBinding(resolver, config, runtime);
    }

    private record RuntimeBinding(
            AgentCardResolver resolver, A2aAgentConfig config, A2aClientRuntime runtime) {
    }

    private static final class RecordingAgentFactory implements A2aAgentFactory {
        private final AtomicInteger created = new AtomicInteger();
        private A2aAgentHandle first;
        private A2aAgentHandle second;
        private A2aClientRequest request;

        @Override
        public A2aAgentHandle create(A2aClientRequest request) {
            this.request = request;
            int sequence = created.incrementAndGet();
            A2aAgentHandle handle = new A2aAgentHandle() {
                @Override
                public Mono<Msg> call(UserMessage message) {
                    return Mono.just(new UserMessage("reply-" + sequence));
                }

                @Override
                public void interrupt() {
                }
            };
            if (sequence == 1) {
                first = handle;
            }
            else {
                second = handle;
            }
            return handle;
        }
    }

    private static final class ConcurrentAgentFactory implements A2aAgentFactory {
        private final AtomicInteger created = new AtomicInteger();
        private final AtomicInteger globalActive = new AtomicInteger();
        private final AtomicInteger maxGlobalActive = new AtomicInteger();
        private final AtomicInteger maxPerAgentActive = new AtomicInteger();
        private final CopyOnWriteArrayList<Sinks.One<Msg>> replies = new CopyOnWriteArrayList<>();

        @Override
        public A2aAgentHandle create(A2aClientRequest request) {
            int sequence = created.incrementAndGet();
            AtomicInteger agentActive = new AtomicInteger();
            Sinks.One<Msg> reply = Sinks.one();
            replies.add(reply);
            return new A2aAgentHandle() {
                @Override
                public Mono<Msg> call(UserMessage message) {
                    return Mono.defer(() -> {
                        int local = agentActive.incrementAndGet();
                        maxPerAgentActive.accumulateAndGet(local, Math::max);
                        int global = globalActive.incrementAndGet();
                        maxGlobalActive.accumulateAndGet(global, Math::max);
                        if (global == 2) {
                            for (int i = 0; i < replies.size(); i++) {
                                replies.get(i).tryEmitValue(new UserMessage("reply-" + (i + 1)));
                            }
                        }
                        return reply.asMono().doFinally(ignored -> {
                            agentActive.decrementAndGet();
                            globalActive.decrementAndGet();
                        });
                    });
                }

                @Override
                public void interrupt() {
                }
            };
        }
    }
}

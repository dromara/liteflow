package com.yomahub.liteflow.agent.a2a;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Creates component-owned A2A runtimes without retaining invocation-scoped agents. */
@FunctionalInterface
public interface A2aClientRuntimeFactory {

    A2aClientRuntime create(AgentCardResolver resolver, A2aAgentConfig config);

    static A2aClientRuntimeFactory defaultFactory() {
        return A2aClientRuntimeFactories.perCall(request -> {
            A2aAgent agent = A2aAgent.builder()
                    .name(request.remoteAgentName())
                    .agentCardResolver(request.resolver())
                    .a2aAgentConfig(request.config())
                    .build();
            return new A2aAgentHandle() {
                @Override
                public Mono<Msg> call(UserMessage message) {
                    return agent.call(List.of(message));
                }

                @Override
                public void interrupt() {
                    agent.interrupt();
                }
            };
        });
    }
}

/** Internal injectable construction path used by package-local contract tests. */
final class A2aClientRuntimeFactories {

    private A2aClientRuntimeFactories() {
    }

    static A2aClientRuntimeFactory perCall(A2aAgentFactory agentFactory) {
        Objects.requireNonNull(agentFactory, "agentFactory");
        return (resolver, config) -> {
            Objects.requireNonNull(resolver, "resolver");
            Objects.requireNonNull(config, "config");
            return request -> Mono.defer(() -> {
                if (request.resolver() != resolver || request.config() != config) {
                    return Mono.error(new IllegalArgumentException(
                            "A2A request resolver/config must match its runtime binding"));
                }
                A2aAgentHandle agent = Objects.requireNonNull(
                        agentFactory.create(request), "agentFactory returned null");
                AtomicBoolean active = new AtomicBoolean(true);
                Mono<Msg> invocation = Mono.defer(() -> Objects.requireNonNull(
                                agent.call(request.message()), "A2A agent call returned null"))
                        .doOnSuccess(ignored -> active.set(false))
                        .doOnError(ignored -> active.set(false))
                        .doOnCancel(() -> interruptOnce(agent, active));
                // AbstractAgentComponent owns the logical deadline; cancellation identifies this agent.
                return invocation;
            });
        };
    }

    private static void interruptOnce(A2aAgentHandle agent, AtomicBoolean active) {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        try {
            agent.interrupt();
        }
        catch (RuntimeException failure) {
            // Cancellation/timeout remains the primary signal; upstream interrupt is best effort.
        }
    }
}

@FunctionalInterface
interface A2aAgentFactory {
    A2aAgentHandle create(A2aClientRequest request);
}

interface A2aAgentHandle {
    Mono<Msg> call(UserMessage message);

    void interrupt();
}

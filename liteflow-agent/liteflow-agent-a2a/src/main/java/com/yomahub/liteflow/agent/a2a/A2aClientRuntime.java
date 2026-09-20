package com.yomahub.liteflow.agent.a2a;

import io.agentscope.core.message.Msg;
import reactor.core.publisher.Mono;

/** Component-owned, call-stateless runtime for remote A2A invocations. */
public interface A2aClientRuntime extends AutoCloseable {

    Mono<Msg> call(A2aClientRequest request);

    @Override
    default void close() {
    }
}

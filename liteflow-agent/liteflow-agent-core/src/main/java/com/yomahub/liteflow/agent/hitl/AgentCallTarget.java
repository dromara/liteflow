package com.yomahub.liteflow.agent.hitl;

import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Mono;

import java.util.List;

/** Provider-neutral Agent call surface used by the shared HITL executor. */
public interface AgentCallTarget {

    Mono<Msg> call(List<Msg> input, RuntimeContext runtimeContext);

    Mono<Msg> call(List<Msg> input, Class<?> javaType, RuntimeContext runtimeContext);

    Mono<Msg> call(List<Msg> input, JsonNode jsonSchema, RuntimeContext runtimeContext);
}

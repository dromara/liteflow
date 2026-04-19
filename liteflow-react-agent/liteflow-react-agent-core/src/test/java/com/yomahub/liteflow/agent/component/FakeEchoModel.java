package com.yomahub.liteflow.agent.component;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Minimal {@link Model} implementation for tests.  Returns a fixed
 * "[echo]" text response regardless of input.
 */
public class FakeEchoModel implements Model {

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
        ChatResponse resp = ChatResponse.builder()
                .id("fake-id")
                .content(List.of(TextBlock.builder().text("[echo]").build()))
                .finishReason("stop")
                .build();
        return Flux.just(resp);
    }

    @Override
    public String getModelName() {
        return "fake-echo-model";
    }
}

package com.yomahub.liteflow.test.agent.support;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Deterministic local model for framework-level agent tests.
 */
public class FakeEchoModel implements Model {

    private final String modelName;

    public FakeEchoModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
        String prompt = messages == null || messages.isEmpty()
                ? ""
                : messages.get(messages.size() - 1).getTextContent();
        return Flux.just(ChatResponse.builder()
                .content(List.of(TextBlock.builder()
                        .text("fake reply: " + (prompt == null ? "" : prompt))
                        .build()))
                .finishReason("stop")
                .build());
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}

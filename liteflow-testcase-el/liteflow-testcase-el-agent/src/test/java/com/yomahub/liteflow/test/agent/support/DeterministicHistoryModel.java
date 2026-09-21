package com.yomahub.liteflow.test.agent.support;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;

/** Deterministic no-network model that records the history presented to each invocation. */
public final class DeterministicHistoryModel implements Model {

    private final String name;
    private final List<Integer> messageCounts;
    private final List<String> messageTexts;

    public DeterministicHistoryModel(String name, List<Integer> messageCounts) {
        this(name, messageCounts, null);
    }

    public DeterministicHistoryModel(
            String name, List<Integer> messageCounts, List<String> messageTexts) {
        this.name = name;
        this.messageCounts = messageCounts;
        this.messageTexts = messageTexts;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        messageCounts.add(messages.size());
        if (messageTexts != null) {
            messageTexts.add(messages.stream()
                    .map(Msg::getTextContent)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining("\n")));
        }
        ContentBlock content = TextBlock.builder().text("deterministic reply").build();
        return Flux.just(ChatResponse.builder()
                .content(List.of(content))
                .finishReason("stop")
                .build());
    }

    @Override
    public String getModelName() {
        return name;
    }
}

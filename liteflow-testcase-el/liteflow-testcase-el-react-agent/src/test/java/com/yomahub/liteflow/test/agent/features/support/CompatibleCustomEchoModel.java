package com.yomahub.liteflow.test.agent.features.support;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 功能测试使用的本地 Echo 模型，避免普通用例依赖真实平台网络。
 */
public class CompatibleCustomEchoModel implements Model {

    private final String nodeId;

    public CompatibleCustomEchoModel(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
        List<String> inputTexts = messages == null ? List.of() : messages.stream()
                .map(Msg::getTextContent)
                .toList();
        return Flux.just(ChatResponse.builder()
                .content(List.of(TextBlock.builder()
                        .text("reply:" + nodeId + ":" + inputTexts)
                        .build()))
                .finishReason("stop")
                .build());
    }

    @Override
    public String getModelName() {
        return "compatible-custom-echo";
    }
}

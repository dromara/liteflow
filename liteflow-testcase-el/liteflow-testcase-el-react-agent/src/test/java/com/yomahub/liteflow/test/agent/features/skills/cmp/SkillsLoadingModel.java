package com.yomahub.liteflow.test.agent.features.skills.cmp;

import com.yomahub.liteflow.test.agent.features.skills.SkillsFeatureProbe;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * skills 功能包的本地模型桩：首轮请求触发 load_skill_through_path，第二轮返回最终文本。
 */
public class SkillsLoadingModel implements Model {
    private final AtomicInteger callCount = new AtomicInteger();

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
        List<String> inputTexts = messages == null ? List.of() : messages.stream()
                .map(Msg::getTextContent)
                .toList();
        List<String> toolNames = toolSchemas == null ? List.of() : toolSchemas.stream()
                .map(ToolSchema::getName)
                .toList();
        SkillsFeatureProbe.TOOL_NAMES.set(toolNames);
        if (callCount.incrementAndGet() == 1 && inputTexts.contains("load-feature-skill")) {
            return Flux.just(ChatResponse.builder()
                    .content(List.of(new ToolUseBlock(
                            "load-feature-demo-call",
                            "load_skill_through_path",
                            Map.of("skillId", "feature-demo_filesystem-features_skills", "path", "SKILL.md"),
                            "{\"skillId\":\"feature-demo_filesystem-features_skills\",\"path\":\"SKILL.md\"}",
                            null)))
                    .finishReason("tool_calls")
                    .build());
        }
        return Flux.just(ChatResponse.builder()
                .content(List.of(TextBlock.builder().text("skill-loaded").build()))
                .finishReason("stop")
                .build());
    }

    @Override
    public String getModelName() {
        return "skill-loading-model";
    }
}

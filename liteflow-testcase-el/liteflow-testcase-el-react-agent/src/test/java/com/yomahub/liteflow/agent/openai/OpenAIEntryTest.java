package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIEntryTest {

    @Test
    void buildsOpenAIChatModelWithGivenModelName() {
        AgentConfig cfg = new AgentConfig();
        cfg.getOpenai().setApiKey("sk-test");

        OpenAISpec spec = OpenAI.of("gpt-4o").temperature(0.7);
        Model model = spec.resolve(cfg);

        assertTrue(model instanceof OpenAIChatModel);
        assertEquals("gpt-4o", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();   // openai credential not set
        OpenAISpec spec = OpenAI.of("gpt-4o");
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> spec.resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"));
    }

    @Test
    void specSettersReturnSubclassType() {
        // 编译期断言：fluent 链返回 OpenAISpec，能链式调用 OpenAI 特有方法
        OpenAISpec spec = OpenAI.of("gpt-4o")
                .temperature(0.7)
                .topP(0.9)
                .reasoningEffort("high")
                .frequencyPenalty(0.1)
                .presencePenalty(0.2);
        assertNotNull(spec);
    }
}

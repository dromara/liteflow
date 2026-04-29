package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiEntryTest {

    @Test
    void buildsGeminiChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getGemini().setApiKey("g-key");

        Model model = Gemini.of("gemini-2.5-pro")
                .temperature(0.6)
                .thinking(t -> t.level("high"))
                .resolve(cfg);

        assertTrue(model instanceof GeminiChatModel);
        assertEquals("gemini-2.5-pro", ((GeminiChatModel) model).getModelName());
    }

    @Test
    void thinkingLevelAndBudgetStored() {
        GeminiSpec spec = Gemini.of("gemini-2.5-pro")
                .thinking(t -> t.level("medium").budget(1024));
        assertEquals("medium", spec.getThinkingLevel());
        assertEquals(1024, spec.getThinkingBudget());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> Gemini.of("gemini-2.5-pro").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.gemini.api-key"));
    }
}

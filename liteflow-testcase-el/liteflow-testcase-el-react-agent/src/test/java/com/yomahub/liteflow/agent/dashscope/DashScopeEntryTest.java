package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeEntryTest {

    @Test
    void buildsDashScopeChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getDashscope().setApiKey("ds-key");

        Model model = DashScope.of("qwen-max")
                .temperature(0.4)
                .thinking(t -> t.budget(2048))
                .resolve(cfg);

        assertTrue(model instanceof DashScopeChatModel);
        assertEquals("qwen-max", ((DashScopeChatModel) model).getModelName());
    }

    @Test
    void thinkingBudgetStored() {
        DashScopeSpec spec = DashScope.of("qwen-max")
                .thinking(t -> t.budget(1024));
        assertEquals(1024, spec.getThinkingBudget());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> DashScope.of("qwen-max").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.dashscope"));
    }
}

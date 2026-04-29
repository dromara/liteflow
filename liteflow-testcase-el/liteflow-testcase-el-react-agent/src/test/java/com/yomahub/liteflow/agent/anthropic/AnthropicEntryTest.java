package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnthropicEntryTest {

    @Test
    void buildsAnthropicChatModel() {
        AgentConfig cfg = new AgentConfig();
        cfg.getAnthropic().setApiKey("ak-test");

        Model model = Anthropic.of("claude-sonnet-4-6")
                .temperature(0.5)
                .thinking(t -> t.budget(2000).enabled(true))
                .resolve(cfg);

        assertTrue(model instanceof AnthropicChatModel);
        assertEquals("claude-sonnet-4-6", ((AnthropicChatModel) model).getModelName());
    }

    @Test
    void throwsWhenApiKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> Anthropic.of("claude-sonnet-4-6").resolve(cfg));
        assertTrue(ex.getMessage().contains("liteflow.agent.anthropic.api-key"));
    }

    @Test
    void compatibleResolvesFromAnthropicCompatibleMap() {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential c = new PlatformCredential();
        c.setApiKey("anc-key");
        c.setBaseUrl("https://my.anthropic-mirror/v1");
        cfg.getAnthropicCompatible().put("mirror", c);

        Model model = AnthropicCompatible.custom("mirror", "claude-haiku")
                .resolve(cfg);
        assertEquals("claude-haiku", ((AnthropicChatModel) model).getModelName());
    }

    @Test
    void compatibleThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> AnthropicCompatible.custom("mirror", "x").resolve(cfg));
        assertTrue(ex.getMessage().contains("anthropic-compatible.mirror"));
    }

    @Test
    void thinkingBuilderStoresBudgetAndEnabled() {
        AnthropicSpec spec = Anthropic.of("claude-sonnet-4-6")
                .thinking(t -> t.budget(1500).enabled(true));
        assertEquals(1500, spec.getThinkingBudget());
        assertEquals(Boolean.TRUE, spec.getThinkingEnabled());
    }
}

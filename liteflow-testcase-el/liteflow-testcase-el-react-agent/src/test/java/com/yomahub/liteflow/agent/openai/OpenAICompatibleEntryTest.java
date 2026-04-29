package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAICompatibleEntryTest {

    private static AgentConfig cfgWith(String key, String apiKey) {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential c = new PlatformCredential();
        c.setApiKey(apiKey);
        cfg.getOpenaiCompatible().put(key, c);
        return cfg;
    }

    @Test
    void deepseekResolvesFromOpenaiCompatibleDeepseek() {
        AgentConfig cfg = cfgWith("deepseek", "ds-key");
        Model model = DeepSeek.of("deepseek-chat").temperature(0.7).resolve(cfg);
        assertTrue(model instanceof OpenAIChatModel);
        assertEquals("deepseek-chat", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void kimiResolvesFromOpenaiCompatibleKimi() {
        AgentConfig cfg = cfgWith("kimi", "kimi-key");
        Model model = Kimi.of("kimi-k2").resolve(cfg);
        assertEquals("kimi-k2", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void glmResolvesFromOpenaiCompatibleGlm() {
        AgentConfig cfg = cfgWith("glm", "glm-key");
        Model model = GLM.of("glm-4").resolve(cfg);
        assertEquals("glm-4", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void minimaxResolvesFromOpenaiCompatibleMinimax() {
        AgentConfig cfg = cfgWith("minimax", "mm-key");
        Model model = Minimax.of("abab6.5").resolve(cfg);
        assertEquals("abab6.5", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void customResolvesFromGivenConfigKey() {
        AgentConfig cfg = cfgWith("myvendor", "my-key");
        PlatformCredential c = cfg.getOpenaiCompatible().get("myvendor");
        c.setBaseUrl("https://my.vendor/v1");

        Model model = OpenAICompatible.custom("myvendor", "my-model").resolve(cfg);
        assertEquals("my-model", ((OpenAIChatModel) model).getModelName());
    }

    @Test
    void customThrowsWhenKeyMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> OpenAICompatible.custom("myvendor", "x").resolve(cfg));
        assertTrue(ex.getMessage().contains("openai-compatible.myvendor"));
    }

    @Test
    void deepseekThrowsWhenCredentialMissing() {
        AgentConfig cfg = new AgentConfig();
        AgentConfigException ex = assertThrows(AgentConfigException.class,
                () -> DeepSeek.of("deepseek-chat").resolve(cfg));
        assertTrue(ex.getMessage().contains("openai-compatible.deepseek"));
    }
}

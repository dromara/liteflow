package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.anthropic.AnthropicCompatible;
import com.yomahub.liteflow.agent.anthropic.AnthropicSpec;
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.dashscope.DashScopeSpec;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.gemini.GeminiSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.agent.openai.GLM;
import com.yomahub.liteflow.agent.openai.Kimi;
import com.yomahub.liteflow.agent.openai.Minimax;
import com.yomahub.liteflow.agent.openai.OpenAI;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.agent.openai.OpenAICompatibleSpec;
import com.yomahub.liteflow.agent.openai.OpenAISpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 覆盖 guide §4 中各平台 ModelSpec 入口、共性参数 getter/fluent setter，
 * 以及缺凭据时 resolve(...) 抛 AgentConfigException 的提示路径。
 */
public class ModelSpecTest {

    @Test
    public void testBaseParamsFluentlyChainAndReturnSelf() {
        OpenAISpec spec = OpenAI.of("gpt-4o-mini");
        OpenAISpec ret = spec.temperature(0.7).topP(0.9).topK(20).maxTokens(123).seed(42L)
                .stream(true).cacheControl(true);
        Assertions.assertSame(spec, ret, "fluent setter 应返回 SELF 类型自身");
        Assertions.assertEquals(0.7, spec.getTemperature());
        Assertions.assertEquals(0.9, spec.getTopP());
        Assertions.assertEquals(20, spec.getTopK());
        Assertions.assertEquals(123, spec.getMaxTokens());
        Assertions.assertEquals(42L, spec.getSeed());
        Assertions.assertEquals(true, spec.getStream());
        Assertions.assertEquals(true, spec.getCacheControl());
        Assertions.assertEquals("gpt-4o-mini", spec.getModelName());
    }

    @Test
    public void testOpenAISpecExposesPlatformSpecificParams() {
        OpenAISpec spec = OpenAI.of("gpt-4o")
                .reasoningEffort("high")
                .frequencyPenalty(0.5)
                .presencePenalty(0.3);
        Assertions.assertEquals("high", spec.getReasoningEffort());
        Assertions.assertEquals(0.5, spec.getFrequencyPenalty());
        Assertions.assertEquals(0.3, spec.getPresencePenalty());
    }

    @Test
    public void testDeepSeekKimiGLMMinimaxAreAllOpenAICompatibleSpec() {
        Assertions.assertInstanceOf(OpenAICompatibleSpec.class, DeepSeek.of("deepseek-chat"));
        Assertions.assertInstanceOf(OpenAICompatibleSpec.class, Kimi.of("moonshot-v1-8k"));
        Assertions.assertInstanceOf(OpenAICompatibleSpec.class, GLM.of("glm-4"));
        Assertions.assertInstanceOf(OpenAICompatibleSpec.class, Minimax.of("MiniMax-Text-01"));
        Assertions.assertInstanceOf(OpenAICompatibleSpec.class,
                OpenAICompatible.custom("vendor", "model"));
    }

    @Test
    public void testOpenAIResolveWithMissingCredentialPointsToConfigPath() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> OpenAI.of("gpt-4o-mini").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai.api-key"));
    }

    @Test
    public void testDeepSeekUsesDefaultBaseUrlWhenNotConfigured() {
        AgentConfig cfg = new AgentConfig();
        PlatformCredential cred = new PlatformCredential();
        cred.setApiKey("test-key");
        cfg.getOpenaiCompatible().put("deepseek", cred);
        Model model = DeepSeek.of("deepseek-chat").resolve(cfg);
        Assertions.assertNotNull(model);
        Assertions.assertEquals("deepseek-chat", model.getModelName());
    }

    @Test
    public void testOpenAICompatibleCustomFailsWhenConfigKeyMissing() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> OpenAICompatible.custom("absent-vendor", "m").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.openai-compatible.absent-vendor.api-key"));
    }

    @Test
    public void testAnthropicSpecExposesThinkingParameters() {
        AnthropicSpec spec = Anthropic.of("claude-3-5-haiku-latest")
                .thinking(t -> t.budget(2000).enabled(true));
        Assertions.assertEquals(2000, spec.getThinkingBudget());
        Assertions.assertEquals(Boolean.TRUE, spec.getThinkingEnabled());
    }

    @Test
    public void testAnthropicResolveWithMissingCredentialPointsToConfigPath() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> Anthropic.of("claude-3-5-haiku-latest").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.anthropic.api-key"));
    }

    @Test
    public void testAnthropicCompatibleUsesAnthropicCompatibleMap() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> AnthropicCompatible.custom("gateway", "claude-haiku").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.anthropic-compatible.gateway.api-key"));
    }

    @Test
    public void testGeminiSpecExposesThinkingParameters() {
        GeminiSpec spec = Gemini.of("gemini-2.5-flash")
                .thinking(t -> t.level("high").budget(1024));
        Assertions.assertEquals("high", spec.getThinkingLevel());
        Assertions.assertEquals(1024, spec.getThinkingBudget());
    }

    @Test
    public void testGeminiResolveWithMissingCredentialPointsToConfigPath() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> Gemini.of("gemini-2.5-flash").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.gemini.api-key"));
    }

    @Test
    public void testDashScopeSpecExposesThinkingParameters() {
        DashScopeSpec spec = DashScope.of("qwen-plus")
                .thinking(t -> t.budget(2048));
        Assertions.assertEquals(2048, spec.getThinkingBudget());
    }

    @Test
    public void testDashScopeResolveWithMissingCredentialPointsToConfigPath() {
        AgentConfigException ex = Assertions.assertThrows(AgentConfigException.class,
                () -> DashScope.of("qwen-plus").resolve(new AgentConfig()));
        Assertions.assertTrue(ex.getMessage().contains("liteflow.agent.dashscope.api-key"));
    }
}

package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicSpecTest {

    @Test
    void realBuilderReceivesResolvedIdentityMergedOptionsAndFormatter() throws Exception {
        AnthropicChatFormatter formatter = new AnthropicChatFormatter();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-test")
                        .apiKey("explicit-key")
                        .baseUrl("https://explicit.example")
                        .stream(false)
                        .temperature(0.25)
                        .topP(0.35)
                        .topK(4)
                        .maxTokens(80)
                        .maxCompletionTokens(70)
                        .seed(9L)
                        .cacheControl(true)
                        .parallelToolCalls(true)
                        .additionalHeader("X-Test", "header-value")
                        .additionalBodyParam("body-key", "body-value")
                        .additionalQueryParam("query-key", "query-value")
                        .thinking(thinking -> thinking.enabled(true).budget(256))
                        .formatter(formatter)
                        .resolve(anthropicConfig("configured-key", "https://configured.example")));

        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals("claude-test", model.getModelName()),
                () -> assertEquals("explicit-key", field(model, "apiKey", String.class)),
                () -> assertEquals("https://explicit.example", field(model, "baseUrl", String.class)),
                () -> assertFalse(field(model, "streamEnabled", Boolean.class)),
                () -> assertSame(formatter, field(model, "formatter", AnthropicChatFormatter.class)),
                () -> assertEquals(Boolean.FALSE, options.getStream()),
                () -> assertEquals(0.25, options.getTemperature()),
                () -> assertEquals(0.35, options.getTopP()),
                () -> assertEquals(4, options.getTopK()),
                () -> assertEquals(80, options.getMaxTokens()),
                () -> assertEquals(70, options.getMaxCompletionTokens()),
                () -> assertEquals(9L, options.getSeed()),
                () -> assertEquals(Boolean.TRUE, options.getCacheControl()),
                () -> assertEquals(Boolean.TRUE, options.getParallelToolCalls()),
                () -> assertEquals(256, options.getThinkingBudget()),
                () -> assertEquals(Map.of("X-Test", "header-value"), options.getAdditionalHeaders()),
                () -> assertEquals(Map.of("body-key", "body-value"), options.getAdditionalBodyParams()),
                () -> assertEquals(Map.of("query-key", "query-value"), options.getAdditionalQueryParams()));
    }

    @Test
    void nativeOptionsOverrideProviderAndCommonOptionsButNotConnectionIdentity() throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("unrelated-native-key")
                .baseUrl("https://unrelated-native.example")
                .modelName("native-model")
                .stream(true)
                .temperature(0.91)
                .thinkingBudget(512)
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("constructor-model")
                        .apiKey("explicit-key")
                        .baseUrl("https://explicit.example")
                        .stream(false)
                        .temperature(0.11)
                        .thinking(thinking -> thinking.enabled(true).budget(256))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals("constructor-model", model.getModelName()),
                () -> assertEquals("explicit-key", field(model, "apiKey", String.class)),
                () -> assertEquals("https://explicit.example", field(model, "baseUrl", String.class)),
                () -> assertTrue(field(model, "streamEnabled", Boolean.class)),
                () -> assertEquals(Boolean.TRUE, options.getStream()),
                () -> assertEquals(0.91, options.getTemperature()),
                () -> assertEquals(512, options.getThinkingBudget()),
                () -> assertEquals("unrelated-native-key", options.getApiKey()),
                () -> assertEquals("https://unrelated-native.example", options.getBaseUrl()),
                () -> assertEquals("native-model", options.getModelName()));
    }

    @Test
    void disabledThinkingOmitsBudgetEvenWhenBudgetWasProvided() throws Exception {
        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-disabled")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(false).budget(256))
                        .resolve(new AgentConfig()));

        assertNull(configuredOptions(model).getThinkingBudget());
    }

    @Test
    void positiveThinkingBudgetEnablesThinkingWhenEnabledWasNotSpecified() throws Exception {
        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-implied")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.budget(256))
                        .resolve(new AgentConfig()));

        assertEquals(256, configuredOptions(model).getThinkingBudget());
    }

    @Test
    void enabledThinkingRequiresPositiveBudget() {
        assertThrows(
                AgentConfigException.class,
                () -> Anthropic.of("claude-thinking-invalid")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true))
                        .resolve(new AgentConfig()));
    }

    @Test
    void nonPositiveThinkingBudgetIsRejectedWhenThinkingIsEnabled() {
        assertThrows(
                AgentConfigException.class,
                () -> Anthropic.of("claude-thinking-invalid")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true).budget(0))
                        .resolve(new AgentConfig()));
    }

    @Test
    void compatibleEndpointRequiresEffectiveBaseUrl() {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey("compatible-key");
        config.setAnthropicCompatible(Map.of("gateway", credential));

        assertThrows(
                AgentConfigException.class,
                () -> AnthropicCompatible.custom("gateway", "claude-compatible").resolve(config));
    }

    @Test
    void compatibleEndpointBuildsWithConfiguredBaseUrl() throws Exception {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey("compatible-key");
        credential.setBaseUrl("https://gateway.example");
        config.setAnthropicCompatible(Map.of("gateway", credential));

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                AnthropicCompatible.custom("gateway", "claude-compatible").resolve(config));

        assertEquals("https://gateway.example", field(model, "baseUrl", String.class));
    }

    @Test
    void customizerRunsLastExactlyOnceAndCanOverrideBuilderSettings() {
        AtomicInteger invocations = new AtomicInteger();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("original-model")
                        .apiKey("test-key")
                        .customizeBuilder(builder -> {
                            invocations.incrementAndGet();
                            builder.modelName("customized-model").stream(false);
                        })
                        .resolve(new AgentConfig()));

        assertAll(
                () -> assertEquals(1, invocations.get()),
                () -> assertEquals("customized-model", model.getModelName()),
                () -> assertFalse(field(model, "streamEnabled", Boolean.class)));
    }

    @Test
    void missingApiKeyFailsBeforeModelConstruction() {
        assertThrows(
                AgentConfigException.class,
                () -> Anthropic.of("claude-missing-key").resolve(new AgentConfig()));
    }

    private static AgentConfig anthropicConfig(String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        config.setAnthropic(credential);
        return config;
    }

    private static GenerateOptions configuredOptions(AnthropicChatModel model) throws Exception {
        return field(model, "defaultOptions", GenerateOptions.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}

package com.yomahub.liteflow.agent.gemini;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.ThinkingConfig;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import io.agentscope.extensions.model.gemini.formatter.GeminiChatFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiSpecTest {

    @Test
    void builderCustomizerCannotBuildAnUnownedGeminiClient() throws Exception {
        AtomicReference<GeminiChatModel> escaped = new AtomicReference<>();
        Model resolved = null;
        try {
            resolved = Gemini.of("gemini-builder-escape")
                    .apiKey("test-key")
                    .customizeBuilder(builder -> {
                        AgentConfigException failure = assertThrows(
                                AgentConfigException.class,
                                () -> escaped.set(builder.build()));
                        assertTrue(failure.getMessage().contains("customizeBuilder"));
                    })
                    .resolve(new AgentConfig());
        } finally {
            close(resolved);
            if (escaped.get() != null) {
                escaped.get().close();
            }
        }
    }

    @Test
    void retainedBuilderCannotBuildAnUnownedGeminiClientAfterResolve() throws Exception {
        AtomicReference<GeminiChatModel.Builder> retained = new AtomicReference<>();
        AtomicReference<GeminiChatModel> escaped = new AtomicReference<>();
        Model resolved = Gemini.of("gemini-retained-builder")
                .apiKey("test-key")
                .customizeBuilder(retained::set)
                .resolve(new AgentConfig());
        try {
            AgentConfigException failure = assertThrows(
                    AgentConfigException.class,
                    () -> escaped.set(retained.get().build()));
            assertTrue(failure.getMessage().contains("customizeBuilder"));
        } finally {
            close(resolved);
            if (escaped.get() != null) {
                escaped.get().close();
            }
        }
    }

    @Test
    void modelFactoryReturnsAnOwningAutoCloseableModel() throws Exception {
        Model model = GeminiModelFactory.of("factory-key", "gemini-factory");
        try {
            assertInstanceOf(AutoCloseable.class, model);
            assertEquals("gemini-factory", model.getModelName());
            ThinkingConfig thinking = requestConfig(
                    model,
                    GenerateOptions.builder().reasoningEffort("high").build())
                    .thinkingConfig()
                    .orElseThrow();
            assertEquals("high", thinking.thinkingLevel().orElseThrow().toString());
        } finally {
            close(model);
        }
    }

    @Test
    void finalRequestConfigContainsDefaultThinkingLevelAndBudget() throws Exception {
        Model model = Gemini.of("gemini-thinking-default")
                .apiKey("test-key")
                .thinking(thinking -> thinking.level("medium").budget(321))
                .resolve(new AgentConfig());
        try {
            ThinkingConfig thinking = requestConfig(model, null)
                    .thinkingConfig()
                    .orElseThrow();

            assertAll(
                    () -> assertEquals("medium", thinking.thinkingLevel().orElseThrow().toString()),
                    () -> assertEquals(321, thinking.thinkingBudget().orElseThrow()),
                    () -> assertTrue(thinking.includeThoughts().orElseThrow()));
        } finally {
            close(model);
        }
    }

    @Test
    void perCallThinkingLevelOverridesDefaultAndKeepsDefaultBudget() throws Exception {
        Model model = Gemini.of("gemini-thinking-per-call")
                .apiKey("test-key")
                .thinking(thinking -> thinking.level("low").budget(222))
                .resolve(new AgentConfig());
        try {
            GenerateOptions perCall = GenerateOptions.builder()
                    .reasoningEffort("high")
                    .build();
            ThinkingConfig thinking = requestConfig(model, perCall)
                    .thinkingConfig()
                    .orElseThrow();

            assertAll(
                    () -> assertEquals("high", thinking.thinkingLevel().orElseThrow().toString()),
                    () -> assertEquals(222, thinking.thinkingBudget().orElseThrow()),
                    () -> assertTrue(thinking.includeThoughts().orElseThrow()));
        } finally {
            close(model);
        }
    }

    @Test
    void perCallThinkingBudgetOverridesDefaultAndKeepsDefaultLevel() throws Exception {
        Model model = Gemini.of("gemini-thinking-per-call-budget")
                .apiKey("test-key")
                .thinking(thinking -> thinking.level("minimal").budget(111))
                .resolve(new AgentConfig());
        try {
            GenerateOptions perCall = GenerateOptions.builder()
                    .thinkingBudget(777)
                    .build();
            ThinkingConfig thinking = requestConfig(model, perCall)
                    .thinkingConfig()
                    .orElseThrow();

            assertAll(
                    () -> assertEquals(
                            "minimal", thinking.thinkingLevel().orElseThrow().toString()),
                    () -> assertEquals(777, thinking.thinkingBudget().orElseThrow()));
        } finally {
            close(model);
        }
    }

    @Test
    void resolvedGeminiModelParticipatesInRuntimeOwnershipContract() throws Exception {
        Model model = Gemini.of("gemini-owned")
                .apiKey("test-key")
                .resolve(new AgentConfig());
        try {
            assertInstanceOf(AutoCloseable.class, model);
        } finally {
            close(model);
        }
    }

    @Test
    void realBuilderReceivesResolvedIdentityMergedOptionsFormatterAndCustomizer()
            throws Exception {
        Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> formatter =
                new GeminiChatFormatter();
        AtomicInteger customizerCalls = new AtomicInteger();
        Model model = null;
        try {
            model = Gemini.of("gemini-constructor")
                            .apiKey("explicit-key")
                            .baseUrl("https://explicit.example")
                            .stream(false)
                            .temperature(0.25)
                            .topP(0.35)
                            .topK(4)
                            .maxTokens(80)
                            .maxCompletionTokens(70)
                            .seed(9L)
                            .cacheControl(false)
                            .parallelToolCalls(false)
                            .additionalHeader("X-Test", "header-value")
                            .additionalBodyParam("body-key", "body-value")
                            .additionalQueryParam("query-key", "query-value")
                            .thinking(thinking -> thinking.level("medium").budget(0))
                            .formatter(formatter)
                            .customizeBuilder(builder -> {
                                customizerCalls.incrementAndGet();
                                builder.modelName("customized-model");
                            })
                            .resolve(geminiConfig("configured-key", "https://configured.example"));

            GeminiChatModel builtModel = geminiDelegate(model);
            GenerateOptions options = defaultOptions(builtModel);
            GeminiThinkingFormatter decoratedFormatter = assertInstanceOf(
                    GeminiThinkingFormatter.class,
                    field(builtModel, "formatter", Formatter.class));
            assertAll(
                    () -> assertEquals("customized-model", builtModel.getModelName()),
                    () -> assertEquals("explicit-key", field(builtModel, "apiKey", String.class)),
                    () -> assertEquals(
                            "https://explicit.example",
                            httpOptions(builtModel).baseUrl().orElseThrow()),
                    () -> assertFalse(field(builtModel, "streamEnabled", Boolean.class)),
                    () -> assertSame(formatter, decoratedFormatter.delegate()),
                    () -> assertEquals(1, customizerCalls.get()),
                    () -> assertEquals(Boolean.FALSE, options.getStream()),
                    () -> assertEquals(0.25, options.getTemperature()),
                    () -> assertEquals(0.35, options.getTopP()),
                    () -> assertEquals(4, options.getTopK()),
                    () -> assertEquals(80, options.getMaxTokens()),
                    () -> assertEquals(70, options.getMaxCompletionTokens()),
                    () -> assertEquals(9L, options.getSeed()),
                    () -> assertEquals(Boolean.FALSE, options.getCacheControl()),
                    () -> assertEquals(Boolean.FALSE, options.getParallelToolCalls()),
                    () -> assertEquals("medium", options.getReasoningEffort()),
                    () -> assertEquals(0, options.getThinkingBudget()),
                    () -> assertEquals(Map.of("X-Test", "header-value"), options.getAdditionalHeaders()),
                    () -> assertEquals(Map.of("body-key", "body-value"), options.getAdditionalBodyParams()),
                    () -> assertEquals(Map.of("query-key", "query-value"), options.getAdditionalQueryParams()));
        } finally {
            close(model);
        }
    }

    @Test
    void nativeOptionsOverrideGenerationValuesButNotConnectionIdentity() throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("native-key")
                .baseUrl("https://native.example")
                .modelName("native-model")
                .stream(true)
                .temperature(0.91)
                .reasoningEffort("native-reasoning")
                .thinkingBudget(123)
                .additionalHeader("native", "header")
                .build();
        Model model = null;
        try {
            model = Gemini.of("constructor-model")
                            .apiKey("explicit-key")
                            .baseUrl("https://explicit.example")
                            .stream(false)
                            .temperature(0.11)
                            .topP(0.22)
                            .thinking(thinking -> thinking.level("provider-reasoning").budget(5))
                            .additionalHeader("common", "header")
                            .generateOptions(nativeOptions)
                            .resolve(geminiConfig("configured-key", "https://configured.example"));

            GeminiChatModel builtModel = geminiDelegate(model);
            GenerateOptions options = defaultOptions(builtModel);
            assertAll(
                    () -> assertEquals("constructor-model", builtModel.getModelName()),
                    () -> assertEquals("explicit-key", field(builtModel, "apiKey", String.class)),
                    () -> assertEquals(
                            "https://explicit.example",
                            httpOptions(builtModel).baseUrl().orElseThrow()),
                    () -> assertEquals(Boolean.TRUE, options.getStream()),
                    () -> assertEquals(0.91, options.getTemperature()),
                    () -> assertEquals(0.22, options.getTopP()),
                    () -> assertEquals("native-reasoning", options.getReasoningEffort()),
                    () -> assertEquals(123, options.getThinkingBudget()),
                    () -> assertEquals(
                            Map.of("common", "header", "native", "header"),
                            options.getAdditionalHeaders()),
                    () -> assertEquals(true, field(builtModel, "streamEnabled", Boolean.class)));
        } finally {
            close(model);
        }
    }

    @Test
    void missingBaseUrlKeepsGeminiExtensionDefaultEndpoint() throws Exception {
        Model model = null;
        try {
            model = Gemini.of("gemini-default-endpoint")
                            .apiKey("explicit-key")
                            .resolve(new AgentConfig());

            assertNull(field(geminiDelegate(model), "httpOptions", HttpOptions.class));
        } finally {
            close(model);
        }
    }

    @Test
    void configuredCredentialsSupplyGeminiConnectionIdentity() throws Exception {
        Model model = null;
        try {
            model = Gemini.of("gemini-configured")
                            .resolve(geminiConfig(
                                    "configured-key", "https://configured.example"));

            GeminiChatModel builtModel = geminiDelegate(model);
            assertAll(
                    () -> assertEquals("configured-key", field(builtModel, "apiKey", String.class)),
                    () -> assertEquals(
                            "https://configured.example",
                            httpOptions(builtModel).baseUrl().orElseThrow()));
        } finally {
            close(model);
        }
    }

    private static AgentConfig geminiConfig(String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        config.setGemini(credential);
        return config;
    }

    private static GenerateContentConfig requestConfig(Model model, GenerateOptions perCall)
            throws Exception {
        GeminiChatModel delegate = geminiDelegate(model);
        Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> formatter =
                field(delegate, "formatter", Formatter.class);
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder();
        formatter.applyOptions(builder, perCall, defaultOptions(delegate));
        return builder.build();
    }

    private static GeminiChatModel geminiDelegate(Model model) throws Exception {
        if (model instanceof GeminiChatModel gemini) {
            return gemini;
        }
        return field(model, "delegate", GeminiChatModel.class);
    }

    private static GenerateOptions defaultOptions(GeminiChatModel model) throws Exception {
        return field(model, "defaultOptions", GenerateOptions.class);
    }

    private static HttpOptions httpOptions(GeminiChatModel model) throws Exception {
        return field(model, "httpOptions", HttpOptions.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void close(Model model) throws Exception {
        if (model instanceof AutoCloseable closeable) {
            closeable.close();
        } else if (model instanceof GeminiChatModel gemini) {
            gemini.close();
        }
    }
}

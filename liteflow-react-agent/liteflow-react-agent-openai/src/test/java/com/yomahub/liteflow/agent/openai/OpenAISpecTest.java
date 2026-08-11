package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.OpenAIClient;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.dto.OpenAIResponse;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
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

class OpenAISpecTest {

    @Test
    void realBuilderReceivesResolvedIdentityMergedOptionsFormatterAndStructuredFlags()
            throws Exception {
        AgentConfig config = openAIConfig("configured-key", "https://configured.example/v1");
        Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter =
                new OpenAIChatFormatter();

        Model resolved = OpenAI.of("gpt-task-3")
                .apiKey("explicit-key")
                .baseUrl("https://explicit.example/v1")
                .endpointPath("/v1/responses")
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
                .reasoningEffort("medium")
                .frequencyPenalty(0.2)
                .presencePenalty(0.3)
                .formatter(formatter)
                .nativeStructuredOutput(false)
                .nativeStructuredOutputWithTools(true)
                .resolve(config);

        OpenAIChatModel model = assertInstanceOf(OpenAIChatModel.class, resolved);
        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals("gpt-task-3", model.getModelName()),
                () -> assertFalse(model.supportsNativeStructuredOutput()),
                () -> assertTrue(model.supportsNativeStructuredOutputWithTools()),
                () -> assertSame(formatter, configuredFormatter(model)),
                () -> assertEquals("explicit-key", options.getApiKey()),
                () -> assertEquals("https://explicit.example/v1", options.getBaseUrl()),
                () -> assertEquals("/v1/responses", options.getEndpointPath()),
                () -> assertEquals("gpt-task-3", options.getModelName()),
                () -> assertEquals(Boolean.FALSE, options.getStream()),
                () -> assertEquals(0.25, options.getTemperature()),
                () -> assertEquals(0.35, options.getTopP()),
                () -> assertEquals(4, options.getTopK()),
                () -> assertEquals(80, options.getMaxTokens()),
                () -> assertEquals(70, options.getMaxCompletionTokens()),
                () -> assertEquals(9L, options.getSeed()),
                () -> assertEquals(Boolean.TRUE, options.getCacheControl()),
                () -> assertEquals(Boolean.TRUE, options.getParallelToolCalls()),
                () -> assertEquals("medium", options.getReasoningEffort()),
                () -> assertEquals(0.2, options.getFrequencyPenalty()),
                () -> assertEquals(0.3, options.getPresencePenalty()),
                () -> assertEquals(Map.of("X-Test", "header-value"),
                        options.getAdditionalHeaders()),
                () -> assertEquals(Map.of("body-key", "body-value"),
                        options.getAdditionalBodyParams()),
                () -> assertEquals(Map.of("query-key", "query-value"),
                        options.getAdditionalQueryParams()));
    }

    @Test
    void nativeOptionsOverrideProviderAndCommonOptionsButNotConnectionIdentity()
            throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("unrelated-native-key")
                .baseUrl("https://unrelated-native.example/v1")
                .endpointPath("/native-endpoint")
                .modelName("native-model")
                .stream(true)
                .temperature(0.91)
                .reasoningEffort("native-reasoning")
                .frequencyPenalty(0.81)
                .build();

        OpenAIChatModel model = assertInstanceOf(
                OpenAIChatModel.class,
                OpenAI.of("constructor-model")
                        .apiKey("explicit-key")
                        .baseUrl("https://explicit.example/v1")
                        .endpointPath("/explicit-endpoint")
                        .stream(false)
                        .temperature(0.11)
                        .topP(0.22)
                        .reasoningEffort("provider-reasoning")
                        .frequencyPenalty(0.31)
                        .presencePenalty(0.41)
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals("explicit-key", options.getApiKey()),
                () -> assertEquals("https://explicit.example/v1", options.getBaseUrl()),
                () -> assertEquals("/explicit-endpoint", options.getEndpointPath()),
                () -> assertEquals("constructor-model", options.getModelName()),
                () -> assertEquals(Boolean.TRUE, options.getStream()),
                () -> assertEquals(0.91, options.getTemperature()),
                () -> assertEquals(0.22, options.getTopP()),
                () -> assertEquals("native-reasoning", options.getReasoningEffort()),
                () -> assertEquals(0.81, options.getFrequencyPenalty()),
                () -> assertEquals(0.41, options.getPresencePenalty()));
    }

    @Test
    void officialDefaultBaseUrlCannotBeReplacedByNativeConnectionOptions()
            throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("unrelated-native-key")
                .baseUrl("https://attacker.invalid")
                .endpointPath("/native-advanced-endpoint")
                .modelName("native-model")
                .build();

        OpenAIChatModel model = assertInstanceOf(
                OpenAIChatModel.class,
                OpenAI.of("constructor-model")
                        .apiKey("official-key")
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals("official-key", options.getApiKey()),
                () -> assertEquals(
                        OpenAIClient.DEFAULT_BASE_URL_WITH_VERSION,
                        options.getBaseUrl()),
                () -> assertEquals("constructor-model", options.getModelName()),
                () -> assertEquals(
                        "/native-advanced-endpoint",
                        options.getEndpointPath()));
    }

    @Test
    void noConfiguredGenerationOptionsLeaveExtensionDefaultsUntouched() throws Exception {
        OpenAIChatModel model = assertInstanceOf(
                OpenAIChatModel.class,
                OpenAI.of("gpt-defaults")
                        .apiKey("explicit-key")
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        assertAll(
                () -> assertEquals(Boolean.TRUE, options.getStream()),
                () -> assertNull(options.getTemperature()),
                () -> assertNull(options.getTopP()),
                () -> assertNull(options.getTopK()),
                () -> assertNull(options.getMaxTokens()),
                () -> assertNull(options.getMaxCompletionTokens()),
                () -> assertNull(options.getSeed()),
                () -> assertNull(options.getCacheControl()),
                () -> assertNull(options.getParallelToolCalls()),
                () -> assertNull(options.getReasoningEffort()),
                () -> assertNull(options.getFrequencyPenalty()),
                () -> assertNull(options.getPresencePenalty()),
                () -> assertTrue(model.supportsNativeStructuredOutput()),
                () -> assertTrue(model.supportsNativeStructuredOutputWithTools()));
    }

    @Test
    void blankOptionalConnectionOverridesUseConfiguredBaseUrlAndNoEndpoint() throws Exception {
        OpenAIChatModel model = assertInstanceOf(
                OpenAIChatModel.class,
                OpenAI.of("gpt-blank-optionals")
                        .apiKey("explicit-key")
                        .baseUrl("   ")
                        .endpointPath("   ")
                        .resolve(openAIConfig(
                                "configured-key", "https://configured.example/v1")));

        GenerateOptions options = configuredOptions(model);
        assertEquals("https://configured.example/v1", options.getBaseUrl());
        assertNull(options.getEndpointPath());
    }

    @Test
    void customizerRunsLastExactlyOnceAndCanOverrideOrdinaryBuilderSettings() {
        AtomicInteger invocations = new AtomicInteger();

        OpenAIChatModel model = assertInstanceOf(
                OpenAIChatModel.class,
                OpenAI.of("original-model")
                        .apiKey("explicit-key")
                        .nativeStructuredOutput(true)
                        .customizeBuilder(builder -> {
                            invocations.incrementAndGet();
                            builder.modelName("customized-model")
                                    .nativeStructuredOutput(false);
                        })
                        .resolve(new AgentConfig()));

        assertAll(
                () -> assertEquals(1, invocations.get()),
                () -> assertEquals("customized-model", model.getModelName()),
                () -> assertFalse(model.supportsNativeStructuredOutput()));
    }

    @Test
    void customizerFailurePropagatesWithoutRetry() {
        AtomicInteger invocations = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("customizer failed");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> OpenAI.of("gpt-error")
                        .apiKey("explicit-key")
                        .customizeBuilder(builder -> {
                            invocations.incrementAndGet();
                            throw failure;
                        })
                        .resolve(new AgentConfig()));

        assertSame(failure, thrown);
        assertEquals(1, invocations.get());
    }

    @Test
    void explicitCredentialsOverrideConfiguredValuesAndBuildExactlyOnce() {
        CapturingOpenAISpec spec = new CapturingOpenAISpec("gpt-captured");
        spec.apiKey("explicit-key").baseUrl("https://explicit.example/v1");

        spec.resolve(openAIConfig("configured-key", "https://configured.example/v1"));

        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("explicit-key", spec.apiKey),
                () -> assertEquals("https://explicit.example/v1", spec.baseUrl));
    }

    @Test
    void explicitCredentialsWorkWithoutConfiguredOpenAIEntry() {
        CapturingOpenAISpec spec = new CapturingOpenAISpec("gpt-explicit-only");
        spec.apiKey("explicit-key").baseUrl("https://explicit.example/v1");

        spec.resolve(new AgentConfig());

        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("explicit-key", spec.apiKey),
                () -> assertEquals("https://explicit.example/v1", spec.baseUrl));
    }

    @Test
    void explicitlyBlankApiKeyFailsWithExactOpenAIConfigPathBeforeBuild() {
        CapturingOpenAISpec spec = new CapturingOpenAISpec("gpt-blank-key");
        spec.apiKey("   ");

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(openAIConfig(
                        "configured-key", "https://configured.example/v1")));

        assertEquals(
                "Missing API key: please configure liteflow.agent.openai.api-key",
                failure.getMessage());
        assertEquals(0, spec.buildCount);
    }

    @Test
    void missingApiKeyFailsWithExactOpenAIConfigPathBeforeBuild() {
        CapturingOpenAISpec spec = new CapturingOpenAISpec("gpt-missing-key");

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> spec.resolve(new AgentConfig()));

        assertEquals(
                "Missing API key: please configure liteflow.agent.openai.api-key",
                failure.getMessage());
        assertEquals(0, spec.buildCount);
    }

    @Test
    void modelFactoryReturnsExtensionModelsWithoutSendingARequest() {
        OpenAIChatModel openAI = OpenAIModelFactory.openai("key", "gpt-factory");
        OpenAIChatModel custom = OpenAIModelFactory.custom(
                "key", "https://custom.example/v1", "custom-factory");

        assertAll(
                () -> assertEquals("gpt-factory", openAI.getModelName()),
                () -> assertEquals("custom-factory", custom.getModelName()));
    }

    private static AgentConfig openAIConfig(String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        config.setOpenai(credential);
        return config;
    }

    // AgentScope 2.0.2 has no public configured-options/formatter accessor. These source-pinned
    // probes inspect the real built extension model instead of bypassing its builder.
    private static GenerateOptions configuredOptions(OpenAIChatModel model) throws Exception {
        return field(model, "configuredOptions", GenerateOptions.class);
    }

    @SuppressWarnings("unchecked")
    private static Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> configuredFormatter(
            OpenAIChatModel model) throws Exception {
        return (Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest>)
                field(model, "formatter", Formatter.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static final class CapturingOpenAISpec extends OpenAISpec {
        private int buildCount;
        private String apiKey;
        private String baseUrl;

        private CapturingOpenAISpec(String modelName) {
            super(modelName);
        }

        @Override
        protected Model buildModel(String apiKey, String baseUrl) {
            buildCount++;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            return null;
        }
    }
}

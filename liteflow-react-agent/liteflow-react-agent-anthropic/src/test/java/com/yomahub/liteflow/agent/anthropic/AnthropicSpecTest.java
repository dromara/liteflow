package com.yomahub.liteflow.agent.anthropic;

import com.anthropic.models.messages.MessageCreateParams;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                () -> assertEquals(Map.of(
                                "body-key", "body-value",
                                "thinking", Map.of(
                                        "type", "enabled",
                                        "budget_tokens", 256)),
                        options.getAdditionalBodyParams()),
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
    void disabledThinkingRemovesNativeBudgetAndBodyButPreservesUnrelatedOptions()
            throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .temperature(0.71)
                .thinkingBudget(512)
                .additionalBodyParam("thinking", Map.of(
                        "type", "enabled",
                        "budget_tokens", 512))
                .additionalBodyParam("native-field", "native-value")
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-disabled-native")
                        .apiKey("test-key")
                        .stream(false)
                        .topP(0.42)
                        .additionalBodyParam("common-field", "common-value")
                        .thinking(thinking -> thinking.enabled(false).budget(256))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        MessageCreateParams request = anthropicRequest(options);
        assertAll(
                () -> assertNull(options.getThinkingBudget()),
                () -> assertEquals(Boolean.FALSE, options.getStream()),
                () -> assertEquals(0.71, options.getTemperature()),
                () -> assertEquals(0.42, options.getTopP()),
                () -> assertEquals("native-value",
                        options.getAdditionalBodyParams().get("native-field")),
                () -> assertEquals("common-value",
                        options.getAdditionalBodyParams().get("common-field")),
                () -> assertFalse(options.getAdditionalBodyParams().containsKey("thinking")),
                () -> assertFalse(request._additionalBodyProperties().containsKey("thinking")));
    }

    @Test
    void nativeNonPositiveThinkingBudgetsAreRejectedAfterMerge() {
        for (int invalidBudget : new int[]{0, -1}) {
            assertThrows(
                    AgentConfigException.class,
                    () -> Anthropic.of("claude-thinking-invalid-native")
                            .apiKey("test-key")
                            .generateOptions(GenerateOptions.builder()
                                    .thinkingBudget(invalidBudget)
                                    .build())
                            .resolve(new AgentConfig()));
        }
    }

    @Test
    void enabledThinkingConsumesPositiveNativeBudgetAndPreservesOtherOptions()
            throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .stream(false)
                .temperature(0.73)
                .thinkingBudget(768)
                .additionalHeader("X-Native", "header-value")
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-native")
                        .apiKey("test-key")
                        .topP(0.44)
                        .thinking(thinking -> thinking.enabled(true))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        Map<String, Object> thinkingBody = bodyObject(anthropicRequest(options), "thinking");
        assertAll(
                () -> assertEquals(768, options.getThinkingBudget()),
                () -> assertEquals(Boolean.FALSE, options.getStream()),
                () -> assertEquals(0.73, options.getTemperature()),
                () -> assertEquals(0.44, options.getTopP()),
                () -> assertEquals(Map.of("X-Native", "header-value"),
                        options.getAdditionalHeaders()),
                () -> assertEquals("enabled", thinkingBody.get("type")),
                () -> assertEquals(768,
                        ((Number) thinkingBody.get("budget_tokens")).intValue()));
    }

    @Test
    void positiveThinkingBudgetReachesAnthropicRequestBody() throws Exception {
        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-request")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true).budget(2048))
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        MessageCreateParams request = anthropicRequest(options);
        assertTrue(request._additionalBodyProperties().containsKey("thinking"));
        Map<String, Object> thinkingBody = bodyObject(request, "thinking");
        assertAll(
                () -> assertEquals(2048, options.getThinkingBudget()),
                () -> assertEquals("enabled", thinkingBody.get("type")),
                () -> assertEquals(2048,
                        ((Number) thinkingBody.get("budget_tokens")).intValue()));
    }

    @Test
    void nativeThinkingBodyWinsTypedBudgetAndKeepsObservationConsistent()
            throws Exception {
        Map<String, Object> nativeThinking = Map.of(
                "type", "enabled",
                "budget_tokens", 3072,
                "vendor-flag", "preserved");
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .thinkingBudget(1024)
                .additionalBodyParam("thinking", nativeThinking)
                .additionalBodyParam("native-extra", "preserved")
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-native-body")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true).budget(512))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        Map<String, Object> requestThinking = bodyObject(anthropicRequest(options), "thinking");
        assertAll(
                () -> assertEquals(3072, options.getThinkingBudget()),
                () -> assertEquals("enabled", requestThinking.get("type")),
                () -> assertEquals(3072,
                        ((Number) requestThinking.get("budget_tokens")).intValue()),
                () -> assertEquals("preserved", requestThinking.get("vendor-flag")),
                () -> assertEquals("preserved",
                        options.getAdditionalBodyParams().get("native-extra")));
    }

    @Test
    void invalidNativeThinkingBodyBudgetIsRejected() {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .thinkingBudget(1024)
                .additionalBodyParam("thinking", Map.of(
                        "type", "enabled",
                        "budget_tokens", 0))
                .build();

        assertThrows(
                AgentConfigException.class,
                () -> Anthropic.of("claude-thinking-invalid-body")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true).budget(512))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));
    }

    @Test
    void fractionalNativeThinkingBodyBudgetIsRejected() {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .additionalBodyParam("thinking", Map.of(
                        "type", "enabled",
                        "budget_tokens", 1.5))
                .build();

        assertThrows(
                AgentConfigException.class,
                () -> Anthropic.of("claude-thinking-fractional-body")
                        .apiKey("test-key")
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));
    }

    @Test
    void integerThinkingBodyBudgetsOutsidePositiveIntRangeAreRejected() {
        Number[] invalidBudgets = {
                (long) Integer.MAX_VALUE + 1,
                new BigInteger("18446744073709551617")
        };

        for (Number invalidBudget : invalidBudgets) {
            GenerateOptions nativeOptions = GenerateOptions.builder()
                    .additionalBodyParam("thinking", Map.of(
                            "type", "enabled",
                            "budget_tokens", invalidBudget))
                    .build();

            assertThrows(
                    AgentConfigException.class,
                    () -> Anthropic.of("claude-thinking-overflow-body")
                            .apiKey("test-key")
                            .generateOptions(nativeOptions)
                            .resolve(new AgentConfig()));
        }
    }

    @Test
    void nonFiniteNativeThinkingBodyBudgetsAreRejected() {
        for (double invalidBudget : new double[]{
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            GenerateOptions nativeOptions = GenerateOptions.builder()
                    .additionalBodyParam("thinking", Map.of(
                            "type", "enabled",
                            "budget_tokens", invalidBudget))
                    .build();

            assertThrows(
                    AgentConfigException.class,
                    () -> Anthropic.of("claude-thinking-non-finite-body")
                            .apiKey("test-key")
                            .generateOptions(nativeOptions)
                            .resolve(new AgentConfig()));
        }
    }

    @Test
    void exactDecimalThinkingBudgetIsCanonicalizedWithoutMutatingUserMap()
            throws Exception {
        BigDecimal userBudget = new BigDecimal("3072.000");
        Map<String, Object> nativeThinking = new LinkedHashMap<>();
        nativeThinking.put("type", "enabled");
        nativeThinking.put("budget_tokens", userBudget);
        nativeThinking.put("vendor-flag", "preserved");
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .additionalBodyParam("thinking", nativeThinking)
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-decimal-body")
                        .apiKey("test-key")
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        Map<?, ?> normalizedBody = assertInstanceOf(
                Map.class, options.getAdditionalBodyParams().get("thinking"));
        Map<String, Object> requestBody = bodyObject(anthropicRequest(options), "thinking");
        assertAll(
                () -> assertEquals(3072, options.getThinkingBudget()),
                () -> assertInstanceOf(Integer.class, normalizedBody.get("budget_tokens")),
                () -> assertEquals(3072, normalizedBody.get("budget_tokens")),
                () -> assertInstanceOf(Integer.class, requestBody.get("budget_tokens")),
                () -> assertEquals(3072, requestBody.get("budget_tokens")),
                () -> assertEquals("preserved", requestBody.get("vendor-flag")),
                () -> assertEquals(userBudget, nativeThinking.get("budget_tokens")));
    }

    @Test
    void nativeDisabledThinkingBodyWinsProviderEnablement() throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .thinkingBudget(1024)
                .additionalBodyParam("thinking", Map.of("type", "disabled"))
                .build();

        AnthropicChatModel model = assertInstanceOf(
                AnthropicChatModel.class,
                Anthropic.of("claude-thinking-native-disabled")
                        .apiKey("test-key")
                        .thinking(thinking -> thinking.enabled(true).budget(512))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = configuredOptions(model);
        Map<String, Object> requestThinking = bodyObject(anthropicRequest(options), "thinking");
        assertAll(
                () -> assertNull(options.getThinkingBudget()),
                () -> assertEquals("disabled", requestThinking.get("type")));
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

    private static MessageCreateParams anthropicRequest(GenerateOptions defaultOptions) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model("claude-test")
                .maxTokens(4096)
                .addUserMessage("offline fixture");
        new AnthropicChatFormatter().applyOptions(builder, null, defaultOptions);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bodyObject(
            MessageCreateParams request, String propertyName) {
        assertNotNull(request._additionalBodyProperties().get(propertyName));
        return request._additionalBodyProperties().get(propertyName).convert(Map.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}

package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.DashScopeHttpClient;
import io.agentscope.extensions.model.dashscope.dto.DashScopeMessage;
import io.agentscope.extensions.model.dashscope.dto.DashScopeRequest;
import io.agentscope.extensions.model.dashscope.dto.DashScopeResponse;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeSpecTest {

    @Test
    void realBuilderUsesCredentialBaseUrlAndFinalCommonOptions() throws Exception {
        Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> formatter =
                new DashScopeChatFormatter();

        DashScopeChatModel model = assertInstanceOf(
                DashScopeChatModel.class,
                DashScope.of("qwen-task-7")
                        .stream(false)
                        .temperature(0.25)
                        .cacheControl(true)
                        .formatter(formatter)
                        .nativeStructuredOutput(true)
                        .nativeStructuredOutputWithTools(true)
                        .resolve(dashScopeConfig(
                                "configured-key", "https://configured.example/v1")));

        GenerateOptions options = defaultOptions(model);
        assertAll(
                () -> assertEquals("qwen-task-7", model.getModelName()),
                () -> assertEquals("configured-key", httpClient(model).apiKey),
                () -> assertEquals(
                        "https://configured.example/v1", httpClient(model).baseUrl),
                () -> assertFalse(field(model, "stream", Boolean.class)),
                () -> assertEquals(Boolean.FALSE, options.getStream()),
                () -> assertEquals(0.25, options.getTemperature()),
                () -> assertEquals(Boolean.TRUE, options.getCacheControl()),
                () -> assertSame(formatter, field(model, "formatter", Formatter.class)),
                () -> assertTrue(model.supportsNativeStructuredOutput()),
                () -> assertTrue(model.supportsNativeStructuredOutputWithTools()));
    }

    @Test
    void nativeOptionsWinWithoutReplacingConnectionIdentity() throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("native-key")
                .baseUrl("https://native.example/v1")
                .modelName("native-model")
                .stream(true)
                .temperature(0.91)
                .cacheControl(false)
                .build();

        DashScopeChatModel model = assertInstanceOf(
                DashScopeChatModel.class,
                DashScope.of("constructor-model")
                        .apiKey("explicit-key")
                        .baseUrl("https://explicit.example/v1")
                        .stream(false)
                        .temperature(0.11)
                        .cacheControl(true)
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        GenerateOptions options = defaultOptions(model);
        assertAll(
                () -> assertEquals("constructor-model", model.getModelName()),
                () -> assertEquals("explicit-key", httpClient(model).apiKey),
                () -> assertEquals("https://explicit.example/v1", httpClient(model).baseUrl),
                () -> assertTrue(field(model, "stream", Boolean.class)),
                () -> assertEquals(Boolean.TRUE, options.getStream()),
                () -> assertEquals(0.91, options.getTemperature()),
                () -> assertEquals(Boolean.FALSE, options.getCacheControl()));
    }

    @Test
    void disabledThinkingIsForwardedAndDoesNotCarryBudget() throws Exception {
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .thinkingBudget(256)
                .build();

        DashScopeChatModel model = assertInstanceOf(
                DashScopeChatModel.class,
                DashScope.of("qwen-thinking-off")
                        .apiKey("fake-key")
                        .thinking(thinking -> thinking.enabled(false).budget(512))
                        .generateOptions(nativeOptions)
                        .resolve(new AgentConfig()));

        assertAll(
                () -> assertEquals(Boolean.FALSE, field(model, "enableThinking", Boolean.class)),
                () -> assertNull(defaultOptions(model).getThinkingBudget()));
    }

    @Test
    void thinkingBudgetEnablesThinkingAndRejectsNonPositiveBudgets() throws Exception {
        DashScopeChatModel model = assertInstanceOf(
                DashScopeChatModel.class,
                DashScope.of("qwen-thinking-on")
                        .apiKey("fake-key")
                        .thinking(thinking -> thinking.budget(512))
                        .resolve(new AgentConfig()));

        assertAll(
                () -> assertEquals(Boolean.TRUE, field(model, "enableThinking", Boolean.class)),
                () -> assertEquals(512, defaultOptions(model).getThinkingBudget()));

        assertThrows(
                IllegalArgumentException.class,
                () -> DashScope.of("qwen-invalid-budget")
                        .apiKey("fake-key")
                        .thinking(thinking -> thinking.budget(0))
                        .resolve(new AgentConfig()));
    }

    @Test
    void customizerRunsLastExactlyOnceAndCanOverrideOrdinaryBuilderSettings() {
        AtomicInteger invocations = new AtomicInteger();

        DashScopeChatModel model = assertInstanceOf(
                DashScopeChatModel.class,
                DashScope.of("original-model")
                        .apiKey("fake-key")
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
    void missingApiKeyFailsWithTheDashScopeConfigurationPath() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> DashScope.of("qwen-missing-key").resolve(new AgentConfig()));

        assertEquals(
                "Missing API key: please configure liteflow.agent.dashscope.api-key",
                failure.getMessage());
    }

    private static AgentConfig dashScopeConfig(String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        config.setDashscope(credential);
        return config;
    }

    private static GenerateOptions defaultOptions(DashScopeChatModel model) throws Exception {
        return field(model, "defaultOptions", GenerateOptions.class);
    }

    private static ClientIdentity httpClient(DashScopeChatModel model) throws Exception {
        DashScopeHttpClient client = field(model, "httpClient", DashScopeHttpClient.class);
        return new ClientIdentity(
                field(client, "apiKey", String.class),
                field(client, "baseUrl", String.class));
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private record ClientIdentity(String apiKey, String baseUrl) {}
}

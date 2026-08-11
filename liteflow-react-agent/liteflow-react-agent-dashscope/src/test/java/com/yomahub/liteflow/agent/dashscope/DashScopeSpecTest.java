package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.DashScopeHttpClient;
import io.agentscope.extensions.model.dashscope.dto.DashScopeMessage;
import io.agentscope.extensions.model.dashscope.dto.DashScopeRequest;
import io.agentscope.extensions.model.dashscope.dto.DashScopeResponse;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

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
    void managedProxyCreatesAnOwnedTransportAndClosesWithoutARequest() throws Exception {
        Model model = DashScope.of("qwen-managed-proxy")
                .apiKey("fake-key")
                .proxy(ProxyConfig.http("localhost", 8083))
                .resolve(new AgentConfig());

        AutoCloseable owner = assertInstanceOf(AutoCloseable.class, model);
        owner.close();
        owner.close();
    }

    @Test
    void explicitOwnedAndBorrowedTransportsHaveDistinctCloseContracts() throws Exception {
        CountingTransport owned = new CountingTransport(null);
        Model ownedModel = DashScope.of("qwen-owned")
                .apiKey("fake-key")
                .ownedHttpTransport(owned)
                .resolve(new AgentConfig());
        assertSame(owned, configuredTransport(dashScopeDelegate(ownedModel)));
        AutoCloseable ownedOwner = assertInstanceOf(AutoCloseable.class, ownedModel);
        ownedOwner.close();
        ownedOwner.close();

        CountingTransport borrowed = new CountingTransport(null);
        Model borrowedModel = DashScope.of("qwen-borrowed")
                .apiKey("fake-key")
                .borrowedHttpTransport(borrowed)
                .resolve(new AgentConfig());
        assertSame(borrowed, configuredTransport(dashScopeDelegate(borrowedModel)));
        if (borrowedModel instanceof AutoCloseable closeable) {
            closeable.close();
        }

        assertAll(
                () -> assertEquals(1, owned.closeCount.get()),
                () -> assertEquals(0, borrowed.closeCount.get()));
    }

    @Test
    void ownedTransportClosesOnBuildRollbackAndSuppressesCloseFailure() {
        IllegalStateException closeFailure = new IllegalStateException("dashscope close failed");
        CountingTransport transport = new CountingTransport(closeFailure);
        IllegalStateException buildFailure = new IllegalStateException("dashscope build failed");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> DashScope.of("qwen-owned-rollback")
                        .apiKey("fake-key")
                        .ownedHttpTransport(transport)
                        .customizeBuilder(builder -> {
                            throw buildFailure;
                        })
                        .resolve(new AgentConfig()));

        assertAll(
                () -> assertSame(buildFailure, thrown),
                () -> assertEquals(1, transport.closeCount.get()),
                () -> assertEquals(1, thrown.getSuppressed().length),
                () -> assertSame(closeFailure, thrown.getSuppressed()[0]));
    }

    @Test
    void builderCustomizerCannotInjectAnAmbiguousTransport() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> DashScope.of("qwen-transport-escape")
                        .apiKey("fake-key")
                        .customizeBuilder(builder ->
                                builder.httpTransport(new CountingTransport(null)))
                        .resolve(new AgentConfig()));

        assertTrue(failure.getMessage().contains("borrowedHttpTransport"));
    }

    @Test
    void builderCustomizerCannotCreateAnUnownedProxyTransport() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> DashScope.of("qwen-proxy-escape")
                        .apiKey("fake-key")
                        .customizeBuilder(builder ->
                                builder.proxy(ProxyConfig.http("localhost", 8383)))
                        .resolve(new AgentConfig()));

        assertTrue(failure.getMessage().contains("proxy"));
    }

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

    private static DashScopeChatModel dashScopeDelegate(Model model) throws Exception {
        if (model instanceof DashScopeChatModel dashScope) {
            return dashScope;
        }
        return field(model, "delegate", DashScopeChatModel.class);
    }

    private static HttpTransport configuredTransport(DashScopeChatModel model) throws Exception {
        DashScopeHttpClient client = field(model, "httpClient", DashScopeHttpClient.class);
        return field(client, "transport", HttpTransport.class);
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

    private static final class CountingTransport implements HttpTransport {
        private final RuntimeException closeFailure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private CountingTransport(RuntimeException closeFailure) {
            this.closeFailure = closeFailure;
        }

        @Override
        public HttpResponse execute(HttpRequest request) {
            throw new AssertionError("offline ownership test must not execute requests");
        }

        @Override
        public Flux<String> stream(HttpRequest request) {
            throw new AssertionError("offline ownership test must not stream requests");
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }
}

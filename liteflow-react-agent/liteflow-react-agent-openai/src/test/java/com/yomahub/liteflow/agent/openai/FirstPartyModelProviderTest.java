package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.CachePolicy;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.compat.deepseek.DeepSeekFormatter;
import io.agentscope.extensions.model.openai.compat.glm.GLMFormatter;
import io.agentscope.extensions.model.openai.compat.kimi.KimiFormatter;
import io.agentscope.extensions.model.openai.compat.minimax.MiniMaxFormatter;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.dto.OpenAIResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstPartyModelProviderTest {

    private static final String COMPATIBLE_PATH = "liteflow.agent.openai-compatible";

    @Test
    void providerManagedProxyAndExplicitTransportOwnershipAreDeterministic()
            throws Exception {
        Model proxied = DeepSeek.of("deepseek-managed-proxy")
                .apiKey("explicit-key")
                .proxy(ProxyConfig.http("localhost", 8082))
                .resolve(new AgentConfig());
        AutoCloseable proxyOwner = assertInstanceOf(AutoCloseable.class, proxied);
        proxyOwner.close();
        proxyOwner.close();

        CountingTransport owned = new CountingTransport(null);
        Model ownedModel = GLM.of("glm-owned")
                .apiKey("explicit-key")
                .ownedHttpTransport(owned)
                .resolve(new AgentConfig());
        assertSame(owned, configuredTransport(openAIDelegate(ownedModel)));
        AutoCloseable ownedOwner = assertInstanceOf(AutoCloseable.class, ownedModel);
        ownedOwner.close();
        ownedOwner.close();

        CountingTransport borrowed = new CountingTransport(null);
        Model borrowedModel = Kimi.of("kimi-borrowed")
                .apiKey("explicit-key")
                .borrowedHttpTransport(borrowed)
                .resolve(new AgentConfig());
        assertSame(borrowed, configuredTransport(openAIDelegate(borrowedModel)));
        if (borrowedModel instanceof AutoCloseable closeable) {
            closeable.close();
        }

        assertAll(
                () -> assertEquals(1, owned.closeCount.get()),
                () -> assertEquals(0, borrowed.closeCount.get()));
    }

    @Test
    void providerOwnedTransportRollsBackAndSuppressesCloseFailure() {
        IllegalStateException closeFailure = new IllegalStateException("provider close failed");
        CountingTransport transport = new CountingTransport(closeFailure);
        IllegalStateException buildFailure = new IllegalStateException("context failed");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> Minimax.of("minimax-owned-rollback")
                        .apiKey("explicit-key")
                        .ownedHttpTransport(transport)
                        .customizeContext(context -> {
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
    void contextCustomizerCannotInjectAnAmbiguousTransport() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> GLM.of("glm-transport-escape")
                        .apiKey("explicit-key")
                        .customizeContext(context -> context.component(
                                HttpTransport.class, new CountingTransport(null)))
                        .resolve(new AgentConfig()));

        assertTrue(failure.getMessage().contains("borrowedHttpTransport"));
    }

    @Test
    void contextCustomizerCannotCreateAnUnownedProxyTransport() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> DeepSeek.of("deepseek-proxy-escape")
                        .apiKey("explicit-key")
                        .customizeContext(context -> context.component(
                                ProxyConfig.class,
                                ProxyConfig.http("localhost", 8282)))
                        .resolve(new AgentConfig()));

        assertTrue(failure.getMessage().contains("proxy"));
    }

    @Test
    void registryDiscoversAllFirstPartyProvidersAndDoesNotCacheCredentialContexts() {
        for (ProviderCase provider : providers()) {
            String modelId = provider.providerId() + ":" + provider.modelName();
            ModelCreationContext context = ModelCreationContext.builder()
                    .apiKey("fake-" + provider.providerId() + "-key")
                    .build();

            assertTrue(ModelRegistry.canResolve(modelId, context), modelId);
            Model first = ModelRegistry.resolve(modelId, context);
            Model second = ModelRegistry.resolve(modelId, context);

            assertAll(
                    modelId,
                    () -> assertInstanceOf(OpenAIChatModel.class, first),
                    () -> assertInstanceOf(OpenAIChatModel.class, second),
                    () -> assertNotSame(first, second));
        }
    }

    @Test
    void factoriesResolveRealProviderModelsWithAuthoritativeIdentityAndMergedOptions()
            throws Exception {
        for (ProviderCase provider : providers()) {
            String fakeKey = "fake-" + provider.providerId() + "-key";
            GenerateOptions nativeOptions = GenerateOptions.builder()
                    .apiKey("unrelated-native-key")
                    .baseUrl("https://unrelated-native.example/v1")
                    .endpointPath("/unrelated-native-endpoint")
                    .modelName("unrelated-native-model")
                    .stream(true)
                    .temperature(0.91)
                    .reasoningEffort("medium")
                    .additionalHeader("native", "header")
                    .build();

            Model resolved = provider.factory()
                    .apply(provider.modelName())
                    .apiKey(fakeKey)
                    .endpointPath("/task-4/chat/completions")
                    .stream(false)
                    .temperature(0.11)
                    .topP(0.22)
                    .additionalHeader("common", "header")
                    .generateOptions(nativeOptions)
                    .resolve(new AgentConfig());

            OpenAIChatModel model = assertInstanceOf(OpenAIChatModel.class, resolved);
            GenerateOptions options = configuredOptions(model);
            assertAll(
                    provider.providerId(),
                    () -> assertEquals(provider.modelName(), model.getModelName()),
                    () -> assertEquals(provider.formatterType(), configuredFormatter(model).getClass()),
                    () -> assertFalse(model.supportsNativeStructuredOutput()),
                    () -> assertFalse(model.supportsNativeStructuredOutputWithTools()),
                    () -> assertEquals(fakeKey, options.getApiKey()),
                    () -> assertEquals(provider.defaultBaseUrl(), options.getBaseUrl()),
                    () -> assertEquals("/task-4/chat/completions", options.getEndpointPath()),
                    () -> assertEquals(provider.modelName(), options.getModelName()),
                    () -> assertEquals(Boolean.TRUE, options.getStream()),
                    () -> assertEquals(0.91, options.getTemperature()),
                    () -> assertEquals(0.22, options.getTopP()),
                    () -> assertEquals("medium", options.getReasoningEffort()),
                    () -> assertEquals(
                            Map.of("native", "header", "common", "header"),
                            options.getAdditionalHeaders()));
        }
    }

    @Test
    void factoriesUseTheirMatchingCompatibleCredentialKeys() throws Exception {
        for (ProviderCase provider : providers()) {
            String fakeKey = "configured-" + provider.providerId() + "-key";
            String baseUrl = "https://" + provider.providerId() + ".configured.example/v1";

            OpenAIChatModel model = assertInstanceOf(
                    OpenAIChatModel.class,
                    provider.factory()
                            .apply(provider.modelName())
                            .resolve(compatibleConfig(provider.providerId(), fakeKey, baseUrl)));

            GenerateOptions options = configuredOptions(model);
            assertAll(
                    provider.providerId(),
                    () -> assertEquals(fakeKey, options.getApiKey()),
                    () -> assertEquals(baseUrl, options.getBaseUrl()));
        }
    }

    @Test
    void realProvidersTranslateThinkingExceptKimiWhichPreservesReasoningOptions()
            throws Exception {
        assertEquals(
                Map.of("thinking", Map.of("type", "enabled")),
                configuredOptions(resolve(DeepSeek.of("deepseek-chat").enableThinking(true)))
                        .getAdditionalBodyParams());
        assertEquals(
                Map.of("thinking", Map.of("type", "disabled")),
                configuredOptions(resolve(GLM.of("glm-4").enableThinking(false)))
                        .getAdditionalBodyParams());
        assertEquals(
                Map.of("thinking", Map.of("type", "adaptive")),
                configuredOptions(resolve(Minimax.of("MiniMax-M2.1").enableThinking(true)))
                        .getAdditionalBodyParams());

        GenerateOptions kimiOptions = GenerateOptions.builder()
                .reasoningEffort("high")
                .additionalBodyParam("sentinel", "preserved")
                .build();
        GenerateOptions resolvedKimi = configuredOptions(resolve(
                Kimi.of("kimi-k2.5")
                        .enableThinking(true)
                        .generateOptions(kimiOptions)));
        assertAll(
                () -> assertEquals("high", resolvedKimi.getReasoningEffort()),
                () -> assertEquals(
                        Map.of("sentinel", "preserved"),
                        resolvedKimi.getAdditionalBodyParams()),
                () -> assertFalse(resolvedKimi.getAdditionalBodyParams().containsKey("thinking")));
    }

    @Test
    void resolveBuildsTheExactModelIdAndCompleteContextExactlyOnce() {
        CapturingOpenAIProviderSpec spec =
                new CapturingOpenAIProviderSpec("deepseek", "deepseek", "deepseek-task-4");
        GenerateOptions nativeOptions = GenerateOptions.builder()
                .apiKey("unrelated-native-key")
                .baseUrl("https://unrelated-native.example/v1")
                .endpointPath("/unrelated-native-endpoint")
                .modelName("unrelated-native-model")
                .stream(true)
                .temperature(0.9)
                .maxCompletionTokens(99)
                .frequencyPenalty(0.3)
                .presencePenalty(0.4)
                .thinkingBudget(123)
                .reasoningEffort("high")
                .additionalHeader("shared", "native")
                .additionalBodyParam("shared", "native")
                .additionalQueryParam("shared", "native")
                .build();
        spec.apiKey("explicit-key")
                .baseUrl("https://explicit.example/v1")
                .endpointPath("/explicit-endpoint")
                .stream(false)
                .temperature(0.1)
                .topP(0.2)
                .topK(3)
                .maxTokens(40)
                .maxCompletionTokens(41)
                .seed(6L)
                .cacheControl(true)
                .parallelToolCalls(false)
                .additionalHeader("common", "header")
                .additionalHeader("shared", "common")
                .additionalBodyParam("common", 1)
                .additionalBodyParam("shared", "common")
                .additionalQueryParam("common", "query")
                .additionalQueryParam("shared", "common")
                .generateOptions(nativeOptions)
                .enableThinking(true);

        spec.resolve(compatibleConfig(
                "deepseek", "configured-key", "https://configured.example/v1"));

        ModelCreationContext context = spec.context;
        GenerateOptions options = context.component(GenerateOptions.class);
        assertAll(
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("deepseek:deepseek-task-4", spec.modelId),
                () -> assertEquals("explicit-key", context.getApiKey()),
                () -> assertEquals("https://explicit.example/v1", context.getBaseUrl()),
                () -> assertEquals("/explicit-endpoint", context.getEndpointPath()),
                () -> assertEquals(Boolean.TRUE, context.getStream()),
                () -> assertEquals(Boolean.TRUE, context.getEnableThinking()),
                () -> assertEquals(CachePolicy.DEFAULT, context.getCachePolicy()),
                () -> assertNull(context.getCacheId()),
                () -> assertTrue(context.getOptions().isEmpty()),
                () -> assertEquals("unrelated-native-key", options.getApiKey()),
                () -> assertEquals("https://unrelated-native.example/v1", options.getBaseUrl()),
                () -> assertEquals("/unrelated-native-endpoint", options.getEndpointPath()),
                () -> assertEquals("unrelated-native-model", options.getModelName()),
                () -> assertEquals(Boolean.TRUE, options.getStream()),
                () -> assertEquals(0.9, options.getTemperature()),
                () -> assertEquals(0.2, options.getTopP()),
                () -> assertEquals(3, options.getTopK()),
                () -> assertEquals(40, options.getMaxTokens()),
                () -> assertEquals(99, options.getMaxCompletionTokens()),
                () -> assertEquals(6L, options.getSeed()),
                () -> assertEquals(Boolean.TRUE, options.getCacheControl()),
                () -> assertEquals(Boolean.FALSE, options.getParallelToolCalls()),
                () -> assertEquals(0.3, options.getFrequencyPenalty()),
                () -> assertEquals(0.4, options.getPresencePenalty()),
                () -> assertEquals(123, options.getThinkingBudget()),
                () -> assertEquals("high", options.getReasoningEffort()),
                () -> assertEquals(
                        Map.of("common", "header", "shared", "native"),
                        options.getAdditionalHeaders()),
                () -> assertEquals(
                        Map.of("common", 1, "shared", "native"),
                        options.getAdditionalBodyParams()),
                () -> assertEquals(
                        Map.of("common", "query", "shared", "native"),
                        options.getAdditionalQueryParams()));
    }

    @Test
    void configuredCredentialsAreUsedAndMissingBaseUrlIsValidForOfficialProviders() {
        CapturingOpenAIProviderSpec configured =
                new CapturingOpenAIProviderSpec("glm", "glm", "glm-task-4");
        configured.resolve(compatibleConfig(
                "glm", "configured-key", "https://configured.example/v1"));
        CapturingOpenAIProviderSpec noBaseUrl =
                new CapturingOpenAIProviderSpec("glm", "glm", "glm-task-4");
        noBaseUrl.apiKey("explicit-key").resolve(new AgentConfig());

        assertAll(
                () -> assertEquals("configured-key", configured.context.getApiKey()),
                () -> assertEquals(
                        "https://configured.example/v1", configured.context.getBaseUrl()),
                () -> assertEquals("explicit-key", noBaseUrl.context.getApiKey()),
                () -> assertNull(noBaseUrl.context.getBaseUrl()),
                () -> assertEquals(1, noBaseUrl.buildCount));
    }

    @Test
    void missingOrBlankCredentialsRetainTheExactCompatibleConfigPathAndDoNotBuild() {
        CapturingOpenAIProviderSpec missing =
                new CapturingOpenAIProviderSpec("minimax", "minimax", "MiniMax-M2.1");
        AgentConfigException missingFailure =
                assertThrows(AgentConfigException.class, () -> missing.resolve(new AgentConfig()));
        CapturingOpenAIProviderSpec blank =
                new CapturingOpenAIProviderSpec("minimax", "minimax", "MiniMax-M2.1");
        AgentConfigException blankFailure = assertThrows(
                AgentConfigException.class,
                () -> blank.resolve(compatibleConfig("minimax", "   ", null)));

        assertAll(
                () -> assertEquals(
                        "Missing platform credential: please configure "
                                + COMPATIBLE_PATH
                                + ".minimax.api-key",
                        missingFailure.getMessage()),
                () -> assertEquals(
                        "Missing API key: please configure "
                                + COMPATIBLE_PATH
                                + ".minimax.api-key",
                        blankFailure.getMessage()),
                () -> assertEquals(0, missing.buildCount),
                () -> assertEquals(0, blank.buildCount));
    }

    @Test
    void contextCustomizerRunsLastExactlyOnceAndCanOverrideOrdinarySettings() {
        AtomicInteger customizerCalls = new AtomicInteger();
        GenerateOptions customizedOptions =
                GenerateOptions.builder().temperature(0.77).build();
        CapturingOpenAIProviderSpec spec =
                new CapturingOpenAIProviderSpec("kimi", "kimi", "kimi-k2.5");
        spec.apiKey("explicit-key")
                .baseUrl("https://explicit.example/v1")
                .endpointPath("/explicit-endpoint")
                .stream(true)
                .enableThinking(true)
                .customizeContext(builder -> {
                    customizerCalls.incrementAndGet();
                    builder.apiKey("customized-key")
                            .baseUrl("https://customized.example/v1")
                            .endpointPath("/customized-endpoint")
                            .stream(false)
                            .enableThinking(false)
                            .component(GenerateOptions.class, customizedOptions);
                });

        spec.resolve(new AgentConfig());

        assertAll(
                () -> assertEquals(1, customizerCalls.get()),
                () -> assertEquals(1, spec.buildCount),
                () -> assertEquals("customized-key", spec.context.getApiKey()),
                () -> assertEquals(
                        "https://customized.example/v1", spec.context.getBaseUrl()),
                () -> assertEquals("/customized-endpoint", spec.context.getEndpointPath()),
                () -> assertEquals(Boolean.FALSE, spec.context.getStream()),
                () -> assertEquals(Boolean.FALSE, spec.context.getEnableThinking()),
                () -> assertSame(
                        customizedOptions, spec.context.component(GenerateOptions.class)));
    }

    @Test
    void contextCustomizerFailurePropagatesBeforeRegistryResolutionWithoutRetry() {
        AtomicInteger customizerCalls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("context customizer failed");
        CapturingOpenAIProviderSpec spec =
                new CapturingOpenAIProviderSpec("kimi", "kimi", "kimi-k2.5");
        spec.apiKey("explicit-key").customizeContext(builder -> {
            customizerCalls.incrementAndGet();
            throw failure;
        });

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, () -> spec.resolve(new AgentConfig()));

        assertAll(
                () -> assertSame(failure, thrown),
                () -> assertEquals(1, customizerCalls.get()),
                () -> assertEquals(0, spec.buildCount));
    }

    @Test
    void constructorRejectsBlankIdentifiersAndProviderPrefixesContainingColon() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new OpenAIProviderSpec(null, "deepseek", "model")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new OpenAIProviderSpec(" ", "deepseek", "model")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new OpenAIProviderSpec("deepseek:", "deepseek", "model")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new OpenAIProviderSpec("deepseek", " ", "model")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new OpenAIProviderSpec("deepseek", "deepseek", " ")));
    }

    private static OpenAIChatModel resolve(OpenAIProviderSpec spec) {
        return assertInstanceOf(
                OpenAIChatModel.class,
                spec.apiKey("fake-provider-key").resolve(new AgentConfig()));
    }

    private static List<ProviderCase> providers() {
        return List.of(
                new ProviderCase(
                        "deepseek",
                        "deepseek-chat",
                        "https://api.deepseek.com",
                        DeepSeekFormatter.class,
                        DeepSeek::of),
                new ProviderCase(
                        "glm",
                        "glm-4",
                        "https://open.bigmodel.cn/api/paas/v4",
                        GLMFormatter.class,
                        GLM::of),
                new ProviderCase(
                        "kimi",
                        "kimi-k2.5",
                        "https://api.moonshot.cn/v1",
                        KimiFormatter.class,
                        Kimi::of),
                new ProviderCase(
                        "minimax",
                        "MiniMax-M2.1",
                        "https://api.minimaxi.com/v1",
                        MiniMaxFormatter.class,
                        Minimax::of));
    }

    private static AgentConfig compatibleConfig(
            String configKey, String apiKey, String baseUrl) {
        AgentConfig config = new AgentConfig();
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey(apiKey);
        credential.setBaseUrl(baseUrl);
        Map<String, PlatformCredential> compatible = new LinkedHashMap<>();
        compatible.put(configKey, credential);
        config.setOpenaiCompatible(compatible);
        return config;
    }

    // AgentScope 2.0.2 exposes no configured-options/formatter accessor. These source-pinned
    // probes inspect the real extension model built by ModelRegistry without sending a request.
    private static GenerateOptions configuredOptions(OpenAIChatModel model) throws Exception {
        return field(model, "configuredOptions", GenerateOptions.class);
    }

    private static OpenAIChatModel openAIDelegate(Model model) throws Exception {
        if (model instanceof OpenAIChatModel openAI) {
            return openAI;
        }
        return field(model, "delegate", OpenAIChatModel.class);
    }

    private static HttpTransport configuredTransport(OpenAIChatModel model) throws Exception {
        return field(model, "client", io.agentscope.extensions.model.openai.OpenAIClient.class)
                .getTransport();
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

    private record ProviderCase(
            String providerId,
            String modelName,
            String defaultBaseUrl,
            Class<? extends Formatter> formatterType,
            Function<String, OpenAIProviderSpec> factory) {}

    private static final class CapturingOpenAIProviderSpec extends OpenAIProviderSpec {
        private int buildCount;
        private String modelId;
        private ModelCreationContext context;

        private CapturingOpenAIProviderSpec(
                String providerId, String configKey, String modelName) {
            super(providerId, configKey, modelName);
        }

        @Override
        protected Model buildModel(String modelId, ModelCreationContext context) {
            buildCount++;
            this.modelId = modelId;
            this.context = context;
            return null;
        }
    }

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

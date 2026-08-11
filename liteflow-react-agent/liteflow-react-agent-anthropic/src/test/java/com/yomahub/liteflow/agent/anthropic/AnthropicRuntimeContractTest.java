package com.yomahub.liteflow.agent.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.runtime.McpClientRegistration;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicBaseFormatter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicRuntimeContractTest {

    private static final String AGENT_NAMESPACE =
            "lf-0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void modelFactoriesReturnOwnersAndSerializePerCallThinkingWithoutDefaults()
            throws Exception {
        Model standard = AnthropicModelFactory.of("test-key", "claude-factory");
        Model custom = AnthropicModelFactory.custom(
                "test-key", "https://anthropic.example", "claude-custom-factory");
        try {
            assertInstanceOf(AutoCloseable.class, standard);
            assertInstanceOf(AutoCloseable.class, custom);
            MessageCreateParams request = request(delegate(standard), GenerateOptions.builder()
                    .maxTokens(2048)
                    .thinkingBudget(1024)
                    .build());
            assertEquals(1024L, request.thinking().orElseThrow()
                    .asEnabled().budgetTokens());
        } finally {
            closeFactoryModel(standard);
            closeFactoryModel(custom);
        }
    }

    @Test
    void builderCustomizerCannotReplaceTheThinkingRequestBoundary() throws Exception {
        Model resolved = Anthropic.of("claude-custom-formatter")
                .apiKey("test-key")
                .maxTokens(4096)
                .thinking(thinking -> thinking.enabled(true).budget(2048))
                .customizeBuilder(builder -> builder.formatter(
                        new io.agentscope.extensions.model.anthropic.formatter
                                .AnthropicChatFormatter()))
                .resolve(new AgentConfig());
        try {
            MessageCreateParams request = request(delegate(resolved), GenerateOptions.builder()
                    .maxTokens(4096)
                    .thinkingBudget(3072)
                    .build());
            assertEquals(3072L, request.thinking().orElseThrow()
                    .asEnabled().budgetTokens());
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void builderCustomizerCannotBuildAnUnownedAnthropicClient() throws Exception {
        AtomicReference<AnthropicChatModel> escaped = new AtomicReference<>();
        Model resolved = null;
        try {
            resolved = Anthropic.of("claude-builder-escape")
                    .apiKey("test-key")
                    .customizeBuilder(builder -> {
                        AgentConfigException failure = assertThrows(
                                AgentConfigException.class,
                                () -> escaped.set(builder.build()));
                        assertTrue(failure.getMessage().contains("customizeBuilder"));
                    })
                    .resolve(new AgentConfig());
        } finally {
            if (resolved instanceof AutoCloseable closeable) {
                closeable.close();
            }
            if (escaped.get() != null) {
                AnthropicClientBridge.close(escaped.get());
            }
        }
    }

    @Test
    void retainedBuilderCannotBuildAnUnownedAnthropicClientAfterResolve() throws Exception {
        AtomicReference<AnthropicChatModel.Builder> retained = new AtomicReference<>();
        AtomicReference<AnthropicChatModel> escaped = new AtomicReference<>();
        Model resolved = Anthropic.of("claude-retained-builder")
                .apiKey("test-key")
                .customizeBuilder(retained::set)
                .resolve(new AgentConfig());
        try {
            AgentConfigException failure = assertThrows(
                    AgentConfigException.class,
                    () -> escaped.set(retained.get().build()));
            assertTrue(failure.getMessage().contains("customizeBuilder"));
        } finally {
            ((AutoCloseable) resolved).close();
            if (escaped.get() != null) {
                AnthropicClientBridge.close(escaped.get());
            }
        }
    }

    @Test
    void callThinkingOverridesDefaultAndReachesTypedAnthropicRequest() throws Exception {
        Model resolved = Anthropic.of("claude-runtime")
                .apiKey("test-key")
                .maxTokens(4096)
                .thinking(thinking -> thinking.enabled(true).budget(2048))
                .resolve(new AgentConfig());
        try {
            assertTrue(resolved instanceof AutoCloseable);
            AnthropicChatModel delegate = delegate(resolved);
            MessageCreateParams request = request(delegate, GenerateOptions.builder()
                    .maxTokens(4096)
                    .thinkingBudget(3072)
                    .build());

            assertTrue(request.thinking().isPresent());
            assertTrue(request.thinking().orElseThrow().isEnabled());
            assertEquals(3072L, request.thinking().orElseThrow().asEnabled().budgetTokens());
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void adaptiveCallThinkingWinsOverDefaultTypedBudget() throws Exception {
        Model resolved = Anthropic.of("claude-adaptive")
                .apiKey("test-key")
                .thinking(thinking -> thinking.enabled(true).budget(2048))
                .resolve(new AgentConfig());
        try {
            MessageCreateParams request = request(delegate(resolved), GenerateOptions.builder()
                    .additionalBodyParam(
                            "thinking", Map.of("type", "adaptive", "vendor", "keep"))
                    .build());

            assertTrue(request.thinking().isPresent());
            assertTrue(request.thinking().orElseThrow().isAdaptive());
            assertEquals("keep", request.thinking().orElseThrow().asAdaptive()
                    ._additionalProperties().get("vendor").convert(String.class));
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void enabledBudgetMustMeetOfficialMinimumAndRemainBelowFinalMaxTokens() throws Exception {
        Model resolved = Anthropic.of("claude-budget")
                .apiKey("test-key")
                .maxTokens(2048)
                .thinking(thinking -> thinking.enabled(true).budget(1024))
                .resolve(new AgentConfig());
        try {
            AnthropicChatModel delegate = delegate(resolved);

            assertThrows(AgentConfigException.class,
                    () -> request(delegate, GenerateOptions.builder()
                            .thinkingBudget(1023)
                            .build()));
            assertThrows(AgentConfigException.class,
                    () -> request(delegate, GenerateOptions.builder()
                            .maxTokens(1024)
                            .thinkingBudget(1024)
                            .build()));
            MessageCreateParams valid = request(delegate, GenerateOptions.builder()
                    .maxTokens(4096)
                    .thinkingBudget(3072)
                    .build());
            assertEquals(3072L, valid.thinking().orElseThrow().asEnabled().budgetTokens());
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void enabledBudgetIsValidatedAgainstMaxTokensAfterCustomFormatter() throws Exception {
        AnthropicBaseFormatter loweringFormatter =
                new io.agentscope.extensions.model.anthropic.formatter
                        .AnthropicChatFormatter() {
                    @Override
                    public void applyOptions(
                            MessageCreateParams.Builder builder,
                            GenerateOptions perCall,
                            GenerateOptions defaults) {
                        super.applyOptions(builder, perCall, defaults);
                        builder.maxTokens(2048);
                    }
                };
        Model resolved = Anthropic.of("claude-final-max")
                .apiKey("test-key")
                .maxTokens(4096)
                .thinking(thinking -> thinking.enabled(true).budget(2048))
                .formatter(loweringFormatter)
                .resolve(new AgentConfig());
        try {
            AgentConfigException failure = assertThrows(
                    AgentConfigException.class,
                    () -> request(delegate(resolved), null));
            assertTrue(failure.getMessage().contains("less than maxTokens"));
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void finalMaxTokensFromCustomFormatterCanMakeEnabledBudgetValid() throws Exception {
        AnthropicBaseFormatter raisingFormatter =
                new io.agentscope.extensions.model.anthropic.formatter
                        .AnthropicChatFormatter() {
                    @Override
                    public void applyOptions(
                            MessageCreateParams.Builder builder,
                            GenerateOptions perCall,
                            GenerateOptions defaults) {
                        super.applyOptions(builder, perCall, defaults);
                        builder.maxTokens(4096);
                    }
                };
        Model resolved = Anthropic.of("claude-final-raised-max")
                .apiKey("test-key")
                .maxTokens(2048)
                .thinking(thinking -> thinking.enabled(true).budget(2048))
                .formatter(raisingFormatter)
                .resolve(new AgentConfig());
        try {
            MessageCreateParams request = request(delegate(resolved), null);
            assertEquals(4096L, request.maxTokens());
            assertEquals(2048L,
                    request.thinking().orElseThrow().asEnabled().budgetTokens());
        } finally {
            ((AutoCloseable) resolved).close();
        }
    }

    @Test
    void runtimeClosesOwnedAnthropicClientOnce() throws Exception {
        AnthropicChatModel delegate = AnthropicChatModel.builder()
                .apiKey("test-key")
                .modelName("claude-owner")
                .build();
        AtomicInteger closes = replaceClient(delegate, new IllegalStateException("close failure"));
        Model owned = AnthropicClientOwner.own(delegate);
        ReActAgent agent = ReActAgent.builder().name("owner-test").model(owned).build();
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        ReActAgentRuntime runtime = new ReActAgentRuntime(
                agent,
                new GuardedNamespacedAgentStateStore(store, AGENT_NAMESPACE),
                new ResolvedAgentStateStore(store, true),
                List.of(owned, owned));

        Throwable failure = assertThrows(RuntimeException.class, runtime::close);
        runtime.close();

        assertEquals(1, closes.get());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }

    @Test
    void buildFailureRollbackClosesOwnedClientAndSuppressesCloseFailure() throws Exception {
        AnthropicChatModel delegate = AnthropicChatModel.builder()
                .apiKey("test-key")
                .modelName("claude-owner-rollback")
                .build();
        IllegalStateException closeFailure = new IllegalStateException("close failure");
        AtomicInteger closes = replaceClient(delegate, closeFailure);
        Model owned = AnthropicClientOwner.own(delegate);
        IllegalArgumentException buildFailure = new IllegalArgumentException("build failure");

        Method rollback = ReActAgentComponent.class.getDeclaredMethod(
                "closeAfterBuildFailure",
                Throwable.class,
                ReActAgent.class,
                List.class,
                List.class,
                List.class,
                GuardedNamespacedAgentStateStore.class,
                ResolvedAgentStateStore.class);
        rollback.setAccessible(true);
        rollback.invoke(null, buildFailure, null, List.<McpClientRegistration>of(),
                List.<AgentSkillRepository>of(), List.of(owned), null, null);

        assertEquals(1, closes.get());
        assertEquals(1, buildFailure.getSuppressed().length);
        assertEquals(closeFailure, buildFailure.getSuppressed()[0]);
    }

    @Test
    void clientBridgeRejectsUnexpectedDelegateBeforeRuntimePublication() {
        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> AnthropicClientBridge.close(new Object()));

        assertTrue(failure.getMessage().contains("AgentScope Anthropic 2.0.2"));
    }

    @Test
    void clientBridgeRejectsFieldAndCloseApiDriftBeforeModelPublication() {
        AgentConfigException fieldDrift = assertThrows(
                AgentConfigException.class,
                () -> AnthropicClientBridge.validateShape(
                        WrongClientFieldModel.class, DriftClient.class));
        AgentConfigException apiDrift = assertThrows(
                AgentConfigException.class,
                () -> AnthropicClientBridge.validateShape(
                        MissingCloseModel.class, MissingCloseClient.class));

        assertTrue(fieldDrift.getMessage().contains("signature changed"));
        assertTrue(apiDrift.getMessage().contains("no longer matches upstream"));
    }

    private static AnthropicChatModel delegate(Model model) throws Exception {
        Field field = model.getClass().getDeclaredField("delegate");
        field.setAccessible(true);
        return assertInstanceOf(AnthropicChatModel.class, field.get(model));
    }

    private static MessageCreateParams request(
            AnthropicChatModel model, GenerateOptions perCall) throws Exception {
        AnthropicBaseFormatter formatter = field(model, "formatter", AnthropicBaseFormatter.class);
        GenerateOptions defaults = field(model, "defaultOptions", GenerateOptions.class);
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model.getModelName())
                .maxTokens(4096)
                .addUserMessage("offline");
        formatter.applyOptions(builder, perCall, defaults);
        return builder.build();
    }

    private static AtomicInteger replaceClient(AnthropicChatModel model, RuntimeException failure)
            throws Exception {
        AtomicInteger closes = new AtomicInteger();
        AnthropicClient replacement = (AnthropicClient) Proxy.newProxyInstance(
                AnthropicClient.class.getClassLoader(),
                new Class<?>[]{AnthropicClient.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        closes.incrementAndGet();
                        throw failure;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        Field field = AnthropicChatModel.class.getDeclaredField("client");
        field.setAccessible(true);
        ((AnthropicClient) field.get(model)).close();
        field.set(model, replacement);
        return closes;
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void closeFactoryModel(Model model) throws Exception {
        if (model instanceof AutoCloseable closeable) {
            closeable.close();
        } else {
            AnthropicClientBridge.close(model);
        }
    }

    private static final class DriftClient {
        public void close() {
        }
    }

    private static final class WrongClientFieldModel {
        @SuppressWarnings("unused")
        private final Object client = new Object();
    }

    private static final class MissingCloseClient {
    }

    private static final class MissingCloseModel {
        @SuppressWarnings("unused")
        private final MissingCloseClient client = new MissingCloseClient();
    }
}

package com.yomahub.liteflow.test.agent.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.agent.openai.GLM;
import com.yomahub.liteflow.agent.openai.Kimi;
import com.yomahub.liteflow.agent.openai.Minimax;
import com.yomahub.liteflow.agent.openai.OpenAI;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.DashScopeHttpClient;
import io.agentscope.extensions.model.dashscope.dto.DashScopeMessage;
import io.agentscope.extensions.model.dashscope.dto.DashScopeRequest;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import io.agentscope.extensions.model.gemini.formatter.GeminiChatFormatter;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

class ProviderClasspathContractTest {

    @Test
    void resolvesEveryProviderToItsRealAgentScopeModelWithoutCallingIt() throws Exception {
        AgentConfig emptyConfig = new AgentConfig();
        List<Model> models = new ArrayList<>();

        try {
            models.add(OpenAI.of("contract-openai")
                    .apiKey("fake-openai-key")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(OpenAICompatible.custom("contract-compatible", "contract-compatible")
                    .apiKey("fake-compatible-key")
                    .baseUrl("https://compatible.invalid/v1")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(DeepSeek.of("contract-deepseek")
                    .apiKey("fake-deepseek-key")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(GLM.of("contract-glm")
                    .apiKey("fake-glm-key")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(Kimi.of("contract-kimi")
                    .apiKey("fake-kimi-key")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(Minimax.of("contract-minimax")
                    .apiKey("fake-minimax-key")
                    .temperature(0.1)
                    .resolve(emptyConfig));
            models.add(Anthropic.of("contract-anthropic")
                    .apiKey("fake-anthropic-key")
                    .resolve(emptyConfig));
            models.add(Gemini.of("contract-gemini")
                    .apiKey("fake-gemini-key")
                    .resolve(emptyConfig));
            models.add(DashScope.of("contract-dashscope")
                    .apiKey("fake-dashscope-key")
                    .resolve(emptyConfig));

            assertAll(
                    () -> assertOpenAIModel(models.get(0), "contract-openai", "fake-openai-key", true, true),
                    () -> assertOpenAIModel(
                            models.get(1), "contract-compatible", "fake-compatible-key", true, true),
                    () -> assertOpenAIModel(
                            models.get(2), "contract-deepseek", "fake-deepseek-key", false, false),
                    () -> assertOpenAIModel(models.get(3), "contract-glm", "fake-glm-key", false, false),
                    () -> assertOpenAIModel(models.get(4), "contract-kimi", "fake-kimi-key", false, false),
                    () -> assertOpenAIModel(
                            models.get(5), "contract-minimax", "fake-minimax-key", false, false),
                    () -> assertProviderModel(
                            models.get(6),
                            "contract-anthropic",
                            "fake-anthropic-key",
                            AnthropicChatModel.class,
                            false,
                            false),
                    () -> assertProviderModel(
                            models.get(7),
                            "contract-gemini",
                            "fake-gemini-key",
                            GeminiChatModel.class,
                            false,
                            false),
                    () -> assertProviderModel(
                            models.get(8),
                            "contract-dashscope",
                            "fake-dashscope-key",
                            DashScopeChatModel.class,
                            false,
                            false));
        } finally {
            closeAll(models);
        }
    }

    @Test
    void linksFormatterOptionsAndTransitiveRuntimeLibrariesWithoutNetworkCalls() throws Exception {
        UserMessage userMessage = new UserMessage("classpath-linkage");
        GenerateOptions defaults = GenerateOptions.builder()
                .temperature(0.2)
                .maxTokens(64)
                .additionalHeader("fallback", "kept")
                .additionalHeader("winner", "default")
                .build();
        GenerateOptions overrides = GenerateOptions.builder()
                .temperature(0.7)
                .topP(0.9)
                .additionalHeader("winner", "override")
                .build();
        GenerateOptions merged = GenerateOptions.mergeOptions(overrides, defaults);

        assertAll(
                () -> assertEquals(0.7, merged.getTemperature()),
                () -> assertEquals(0.9, merged.getTopP()),
                () -> assertEquals(64, merged.getMaxTokens()),
                () -> assertEquals(
                        Map.of("fallback", "kept", "winner", "override"),
                        merged.getAdditionalHeaders()),
                () -> assertEquals(0.2, defaults.getTemperature()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> merged.getAdditionalHeaders().put("mutation", "rejected")));

        OpenAIChatFormatter openAIFormatter = new OpenAIChatFormatter();
        List<OpenAIMessage> openAIMessages = openAIFormatter.format(List.of(userMessage));
        OpenAIRequest openAIRequest = new OpenAIRequest();
        openAIFormatter.applyOptions(openAIRequest, overrides, defaults);

        AnthropicChatFormatter anthropicFormatter = new AnthropicChatFormatter();
        List<MessageParam> anthropicMessages = anthropicFormatter.format(List.of(userMessage));
        MessageCreateParams.Builder anthropicBuilder = MessageCreateParams.builder()
                .model("contract-anthropic")
                .maxTokens(1)
                .messages(anthropicMessages);
        anthropicFormatter.applyOptions(anthropicBuilder, overrides, defaults);
        anthropicFormatter.applyTools(anthropicBuilder, List.of());
        MessageCreateParams anthropicParams = anthropicBuilder.build();

        GeminiChatFormatter geminiFormatter = new GeminiChatFormatter();
        List<Content> geminiMessages = geminiFormatter.format(List.of(userMessage));
        GenerateContentConfig.Builder geminiBuilder = GenerateContentConfig.builder();
        geminiFormatter.applyOptions(geminiBuilder, overrides, defaults);
        GenerateContentConfig geminiConfig = geminiBuilder.build();

        DashScopeChatFormatter dashScopeFormatter = new DashScopeChatFormatter();
        List<DashScopeMessage> dashScopeMessages = dashScopeFormatter.format(List.of(userMessage));
        DashScopeRequest dashScopeRequest =
                dashScopeFormatter.buildRequest("contract-dashscope", dashScopeMessages, false);
        dashScopeFormatter.applyOptions(dashScopeRequest, overrides, defaults);

        assertAll(
                () -> assertEquals("user", openAIMessages.get(0).getRole()),
                () -> assertEquals(0.7, openAIRequest.getTemperature()),
                () -> assertEquals(0.9, openAIRequest.getTopP()),
                () -> assertEquals(64, openAIRequest.getMaxTokens()),
                () -> assertEquals(1, anthropicMessages.size()),
                () -> assertEquals(0.7, anthropicParams.temperature().orElseThrow()),
                () -> assertEquals(0.9, anthropicParams.topP().orElseThrow()),
                () -> assertEquals(64, anthropicParams.maxTokens()),
                () -> assertEquals("user", geminiMessages.get(0).role().orElseThrow()),
                () -> assertEquals(0.7F, geminiConfig.temperature().orElseThrow()),
                () -> assertEquals(0.9F, geminiConfig.topP().orElseThrow()),
                () -> assertEquals(64, geminiConfig.maxOutputTokens().orElseThrow()),
                () -> assertEquals("user", dashScopeMessages.get(0).getRole()),
                () -> assertEquals(0.7, dashScopeRequest.getParameters().getTemperature()),
                () -> assertEquals(0.9, dashScopeRequest.getParameters().getTopP()),
                () -> assertEquals(64, dashScopeRequest.getParameters().getMaxTokens()));

        Content googleValue = Content.builder()
                .role("user")
                .parts(Part.fromText("google-sdk-linkage"))
                .build();
        Message dashScopeValue = Message.builder()
                .role(Role.USER.getValue())
                .content("dashscope-sdk-linkage")
                .build();
        String json = new ObjectMapper().writeValueAsString(
                Map.of("message", userMessage, "options", merged));
        Mono<String> reactorAssembly = Mono.just("reactor-linkage")
                .map(String::toUpperCase)
                .checkpoint("provider-classpath-contract");
        Logger logger = LoggerFactory.getLogger(ProviderClasspathContractTest.class);

        assertAll(
                () -> assertEquals("google-sdk-linkage", googleValue.text()),
                () -> assertEquals("dashscope-sdk-linkage", dashScopeValue.getContent()),
                () -> assertTrue(json.contains("classpath-linkage")),
                () -> assertTrue(json.contains("\"temperature\":0.7")),
                () -> assertNotNull(reactorAssembly),
                () -> assertFalse(reactorAssembly.toString().isBlank()),
                () -> assertEquals(ProviderClasspathContractTest.class.getName(), logger.getName()));

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(2))
                .build();
        try {
            Request request = new Request.Builder()
                    .url("https://offline.invalid/provider-linkage")
                    .header("X-Linkage-Test", "true")
                    .build();
            assertAll(
                    () -> assertEquals("offline.invalid", request.url().host()),
                    () -> assertEquals("true", request.header("X-Linkage-Test")),
                    () -> assertEquals(2_000, httpClient.callTimeoutMillis()));
        } finally {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }
    }

    private static void assertOpenAIModel(
            Model model,
            String modelName,
            String apiKey,
            boolean structured,
            boolean structuredWithTools) throws Exception {
        OpenAIChatModel openAIModel = assertInstanceOf(OpenAIChatModel.class, model);
        GenerateOptions options = field(openAIModel, "configuredOptions", GenerateOptions.class);
        assertAll(
                () -> assertEquals(modelName, openAIModel.getModelName()),
                () -> assertEquals(apiKey, options.getApiKey()),
                () -> assertEquals(structured, openAIModel.supportsNativeStructuredOutput()),
                () -> assertEquals(
                        structuredWithTools,
                        openAIModel.supportsNativeStructuredOutputWithTools()));
    }

    private static void assertProviderModel(
            Model model,
            String modelName,
            String apiKey,
            Class<? extends Model> expectedType,
            boolean structured,
            boolean structuredWithTools) throws Exception {
        Model providerModel = assertInstanceOf(expectedType, model);
        String configuredApiKey = configuredApiKey(providerModel);
        assertAll(
                () -> assertEquals(modelName, providerModel.getModelName()),
                () -> assertEquals(apiKey, configuredApiKey),
                () -> assertEquals(structured, providerModel.supportsNativeStructuredOutput()),
                () -> assertEquals(
                        structuredWithTools,
                        providerModel.supportsNativeStructuredOutputWithTools()));
    }

    private static String configuredApiKey(Model model) throws Exception {
        if (model instanceof DashScopeChatModel dashScopeModel) {
            DashScopeHttpClient client = field(dashScopeModel, "httpClient", DashScopeHttpClient.class);
            return field(client, "apiKey", String.class);
        }
        return field(model, "apiKey", String.class);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static void closeAll(List<Model> models) throws Exception {
        Exception failure = null;
        for (Model model : models) {
            if (model instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}

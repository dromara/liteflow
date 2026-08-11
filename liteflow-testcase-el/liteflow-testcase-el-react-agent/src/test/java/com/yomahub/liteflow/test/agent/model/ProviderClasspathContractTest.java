package com.yomahub.liteflow.test.agent.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.dashscope.DashScopeHttpClient;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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

package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;

import java.util.function.Consumer;

/**
 * OpenAI-compatible first-party provider spec resolved through AgentScope's model registry.
 */
public class OpenAIProviderSpec extends ModelSpec<OpenAIProviderSpec> {

    private static final String COMPATIBLE_CONFIG_PATH =
            "liteflow.agent.openai-compatible";

    private final String providerId;
    private final String configKey;
    private final String modelName;
    private String endpointPath;
    private Boolean enableThinking;
    private Consumer<ModelCreationContext.Builder> contextCustomizer;

    public OpenAIProviderSpec(String providerId, String configKey, String modelName) {
        this.providerId = requireIdentifier(providerId, "providerId");
        if (providerId.indexOf(':') >= 0) {
            throw new IllegalArgumentException("providerId must not contain ':'");
        }
        this.configKey = requireIdentifier(configKey, "configKey");
        this.modelName = requireIdentifier(modelName, "modelName");
    }

    public OpenAIProviderSpec endpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
        return this;
    }

    public OpenAIProviderSpec enableThinking(boolean enabled) {
        this.enableThinking = enabled;
        return this;
    }

    public OpenAIProviderSpec customizeContext(
            Consumer<ModelCreationContext.Builder> customizer) {
        this.contextCustomizer = customizer;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveCompatible(
                        cfg.getOpenaiCompatible(),
                        configKey,
                        COMPATIBLE_CONFIG_PATH,
                        getApiKey(),
                        getBaseUrl());
        GenerateOptions options = mergeGenerateOptions(null);
        ModelCreationContext.Builder context = ModelCreationContext.builder()
                .apiKey(credential.apiKey())
                .baseUrl(credential.baseUrl())
                .endpointPath(endpointPath)
                .stream(options == null ? null : options.getStream())
                .enableThinking(enableThinking)
                .component(GenerateOptions.class, options);
        if (contextCustomizer != null) {
            contextCustomizer.accept(context);
        }
        return buildModel(providerId + ":" + modelName, context.build());
    }

    protected Model buildModel(String modelId, ModelCreationContext context) {
        return ModelRegistry.resolve(modelId, context);
    }

    private static String requireIdentifier(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

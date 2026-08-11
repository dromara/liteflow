package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.model.OwnedTransportModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.OkHttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;

import java.util.Objects;
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
    private ProxyConfig proxyConfig;
    private HttpTransport httpTransport;
    private boolean ownsHttpTransport;
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

    /** Configures a proxy transport that is created and closed by the resolved model. */
    public OpenAIProviderSpec proxy(ProxyConfig proxyConfig) {
        this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
        return this;
    }

    /** Uses a caller-owned transport; LiteFlow never closes it. */
    public OpenAIProviderSpec borrowedHttpTransport(HttpTransport httpTransport) {
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport");
        this.ownsHttpTransport = false;
        return this;
    }

    /** Transfers transport ownership to the resolved model and runtime. */
    public OpenAIProviderSpec ownedHttpTransport(HttpTransport httpTransport) {
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport");
        this.ownsHttpTransport = true;
        return this;
    }

    /**
     * Runs last for ordinary context settings. Transport and proxy ownership must be declared
     * through this spec's explicit ownership methods.
     */
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
        HttpTransport ownedTransport = ownsHttpTransport ? httpTransport : null;
        try {
            GenerateOptions options = mergeGenerateOptions(null);
            ModelCreationContext.Builder context = ModelCreationContext.builder()
                    .apiKey(credential.apiKey())
                    .baseUrl(credential.baseUrl())
                    .endpointPath(endpointPath)
                    .stream(options == null ? null : options.getStream())
                    .enableThinking(enableThinking)
                    .component(GenerateOptions.class, options)
                    .component(HttpTransport.class, httpTransport)
                    .component(ProxyConfig.class, proxyConfig);
            if (contextCustomizer != null) {
                contextCustomizer.accept(context);
            }
            ModelCreationContext customized = context.build();
            validateOwnershipComponents(customized);

            HttpTransport finalTransport = customized.component(HttpTransport.class);
            ProxyConfig finalProxy = customized.component(ProxyConfig.class);
            ModelCreationContext effectiveContext = customized;
            if (finalTransport != null) {
                effectiveContext = customized.toBuilder()
                        .component(ProxyConfig.class, null)
                        .build();
            } else if (finalProxy != null) {
                ownedTransport = OkHttpTransport.builder()
                        .config(HttpTransportConfig.builder().proxy(finalProxy).build())
                        .build();
                effectiveContext = customized.toBuilder()
                        .component(HttpTransport.class, ownedTransport)
                        .component(ProxyConfig.class, null)
                        .build();
            }

            Model model = buildModel(providerId + ":" + modelName, effectiveContext);
            return ownedTransport == null
                    ? model
                    : new OwnedTransportModel(model, ownedTransport);
        } catch (RuntimeException | Error failure) {
            OwnedTransportModel.closeAfterBuildFailure(ownedTransport, failure);
            throw failure;
        }
    }

    protected Model buildModel(String modelId, ModelCreationContext context) {
        return ModelRegistry.resolve(modelId, context);
    }

    private void validateOwnershipComponents(ModelCreationContext customized) {
        HttpTransport customizedTransport = customized.component(HttpTransport.class);
        if (customizedTransport != httpTransport) {
            throw new AgentConfigException(
                    "customizeContext cannot replace an HTTP transport with ambiguous ownership; "
                            + "use borrowedHttpTransport(...) or ownedHttpTransport(...)");
        }
        ProxyConfig customizedProxy = customized.component(ProxyConfig.class);
        if (!Objects.equals(customizedProxy, proxyConfig)) {
            throw new AgentConfigException(
                    "customizeContext cannot create an unowned proxy transport; "
                            + "use OpenAIProviderSpec.proxy(...)");
        }
    }

    private static String requireIdentifier(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

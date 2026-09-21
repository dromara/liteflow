package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.model.OwnedTransportModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.OkHttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.OpenAIClient;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.dto.OpenAIResponse;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * OpenAI 系（含 OpenAI 兼容族）通用 spec。
 * 暴露 OpenAI 平台特有的 reasoningEffort / frequencyPenalty / presencePenalty 等参数。
 */
public class OpenAISpec extends ModelSpec<OpenAISpec> {

    private final String modelName;
    private String reasoningEffort;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private String endpointPath;
    private Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter;
    private Boolean nativeStructuredOutput;
    private Boolean nativeStructuredOutputWithTools;
    private ProxyConfig proxyConfig;
    private HttpTransport httpTransport;
    private boolean ownsHttpTransport;
    private Consumer<OpenAIChatModel.Builder> builderCustomizer;

    public OpenAISpec(String modelName) {
        this.modelName = modelName;
    }

    public OpenAISpec reasoningEffort(String level) { this.reasoningEffort = level; return this; }
    public OpenAISpec frequencyPenalty(double v)    { this.frequencyPenalty = v;   return this; }
    public OpenAISpec presencePenalty(double v)     { this.presencePenalty = v;    return this; }
    public OpenAISpec endpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
        return this;
    }
    public OpenAISpec formatter(
            Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter) {
        this.formatter = formatter;
        return this;
    }
    public OpenAISpec nativeStructuredOutput(boolean enabled) {
        this.nativeStructuredOutput = enabled;
        return this;
    }
    public OpenAISpec nativeStructuredOutputWithTools(boolean enabled) {
        this.nativeStructuredOutputWithTools = enabled;
        return this;
    }

    /** Configures a proxy transport that is created and closed by the resolved model. */
    public OpenAISpec proxy(ProxyConfig proxyConfig) {
        this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
        return this;
    }

    /** Uses a caller-owned transport; LiteFlow never closes it. */
    public OpenAISpec borrowedHttpTransport(HttpTransport httpTransport) {
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport");
        this.ownsHttpTransport = false;
        return this;
    }

    /** Transfers transport ownership to the resolved model and runtime. */
    public OpenAISpec ownedHttpTransport(HttpTransport httpTransport) {
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport");
        this.ownsHttpTransport = true;
        return this;
    }

    /**
     * Runs last for ordinary builder settings. Transport and proxy ownership must be declared
     * through {@link #borrowedHttpTransport(HttpTransport)},
     * {@link #ownedHttpTransport(HttpTransport)}, or {@link #proxy(ProxyConfig)}.
     */
    public OpenAISpec customizeBuilder(Consumer<OpenAIChatModel.Builder> customizer) {
        this.builderCustomizer = customizer;
        return this;
    }

    public String getModelName()         { return modelName; }
    public String getReasoningEffort()   { return reasoningEffort; }
    public Double getFrequencyPenalty()  { return frequencyPenalty; }
    public Double getPresencePenalty()   { return presencePenalty; }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveFirstClass(
                        cfg.getOpenai(),
                        "liteflow.agent.openai",
                        getApiKey(),
                        getBaseUrl());
        return recordMetadata(buildModel(credential.apiKey(), credential.baseUrl()), "openai", credential.baseUrl());
    }

    /** 子类（OpenAICompatibleSpec）可覆盖以提供不同 baseUrl / apiKey 来源。 */
    protected Model buildModel(String apiKey, String baseUrl) {
        ManagedOpenAIBuilder builder =
                new ManagedOpenAIBuilder(httpTransport, proxyConfig);
        HttpTransport ownedTransport = ownsHttpTransport ? httpTransport : null;
        try {
            builder.apiKey(apiKey)
                    .baseUrl(baseUrl != null && !baseUrl.isBlank()
                            ? baseUrl
                            : OpenAIClient.DEFAULT_BASE_URL_WITH_VERSION)
                    .modelName(modelName);
            if (endpointPath != null && !endpointPath.isBlank()) {
                builder.endpointPath(endpointPath);
            }
            GenerateOptions options = mergeGenerateOptions(buildGenerateOptions());
            if (options != null) {
                builder.generateOptions(options);
                if (options.getStream() != null) {
                    builder.stream(options.getStream());
                }
            }
            if (formatter != null) {
                builder.formatter(formatter);
            }
            if (nativeStructuredOutput != null) {
                builder.nativeStructuredOutput(nativeStructuredOutput);
            }
            if (nativeStructuredOutputWithTools != null) {
                builder.nativeStructuredOutputWithTools(nativeStructuredOutputWithTools);
            }
            if (builderCustomizer != null) {
                builderCustomizer.accept(builder);
            }
            HttpTransport managedProxyTransport = builder.prepareManagedProxyTransport();
            if (managedProxyTransport != null) {
                ownedTransport = managedProxyTransport;
            }
            Model model = builder.buildManaged();
            return ownedTransport == null
                    ? model
                    : new OwnedTransportModel(model, ownedTransport);
        } catch (RuntimeException | Error failure) {
            OwnedTransportModel.closeAfterBuildFailure(ownedTransport, failure);
            throw failure;
        }
    }

    /** 把 OpenAI 个性参数装配成 GenerateOptions；全部为 null 时返回 null。 */
    protected GenerateOptions buildGenerateOptions() {
        if (reasoningEffort == null
                && frequencyPenalty == null && presencePenalty == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (reasoningEffort != null)   b.reasoningEffort(reasoningEffort);
        if (frequencyPenalty != null)  b.frequencyPenalty(frequencyPenalty);
        if (presencePenalty != null)   b.presencePenalty(presencePenalty);
        return b.build();
    }

    private static final class ManagedOpenAIBuilder extends OpenAIChatModel.Builder {
        private final HttpTransport declaredTransport;
        private final ProxyConfig declaredProxy;
        private boolean built;

        private ManagedOpenAIBuilder(
                HttpTransport declaredTransport, ProxyConfig declaredProxy) {
            this.declaredTransport = declaredTransport;
            this.declaredProxy = declaredProxy;
            if (declaredTransport != null) {
                super.httpTransport(declaredTransport);
            }
        }

        @Override
        public OpenAIChatModel.Builder httpTransport(HttpTransport transport) {
            if (transport != declaredTransport) {
                throw new AgentConfigException(
                        "customizeBuilder cannot inject an ambiguous HTTP transport; "
                                + "use borrowedHttpTransport(...) or ownedHttpTransport(...)");
            }
            return super.httpTransport(transport);
        }

        @Override
        public OpenAIChatModel.Builder proxy(ProxyConfig proxy) {
            if (!Objects.equals(proxy, declaredProxy)) {
                throw new AgentConfigException(
                        "customizeBuilder cannot create an unowned proxy transport; "
                                + "use OpenAISpec.proxy(...)");
            }
            return this;
        }

        @Override
        public OpenAIChatModel build() {
            throw escapedBuild();
        }

        private OpenAIChatModel buildManaged() {
            if (built) {
                throw escapedBuild();
            }
            built = true;
            return super.build();
        }

        private HttpTransport prepareManagedProxyTransport() {
            if (declaredTransport != null || declaredProxy == null) {
                return null;
            }
            HttpTransport managed = OkHttpTransport.builder()
                    .config(HttpTransportConfig.builder().proxy(declaredProxy).build())
                    .build();
            super.httpTransport(managed);
            return managed;
        }

        private static AgentConfigException escapedBuild() {
            return new AgentConfigException(
                    "customizeBuilder cannot call or retain builder.build(); "
                            + "LiteFlow owns OpenAI model construction");
        }
    }
}

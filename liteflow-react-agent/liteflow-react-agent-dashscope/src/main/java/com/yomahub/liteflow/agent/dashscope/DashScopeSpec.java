package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class DashScopeSpec extends ModelSpec<DashScopeSpec> {

    private final String modelName;
    private Integer thinkingBudget;

    public DashScopeSpec(String modelName) { this.modelName = modelName; }

    public DashScopeSpec thinking(Consumer<DashScopeThinking> c) {
        DashScopeThinking t = new DashScopeThinking();
        c.accept(t);
        this.thinkingBudget = t.getBudget();
        return this;
    }

    public String  getModelName()      { return modelName; }
    public Integer getThinkingBudget() { return thinkingBudget; }

    @Override
    public Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getDashscope(), "liteflow.agent.dashscope");

        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.defaultOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        if (thinkingBudget != null) {
            builder.enableThinking(true);
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (getCacheControl() != null) b.cacheControl(getCacheControl());
        if (thinkingBudget != null)    b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}

package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示 guide §4.6 buildModel() 逃生舱：完全自行构造 agentscope Model。
 * 这里仍然使用 compatible-custom 的 baseUrl/apiKey，但绕过 ModelSpec 而直接
 * 调用 {@link OpenAIChatModel#builder()}。
 */
@Component("buildModelEscapeAgent")
public class BuildModelEscapeAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();

    public static void reset() {
        BUILD_MODEL_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        // model() 仍是抽象，必须实现；但 buildModel() 覆写后 resolve 不会被调用。
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, "unused-model-spec");
    }

    @Override
    protected Model buildModel() {
        BUILD_MODEL_COUNT.incrementAndGet();
        PlatformCredential cred = agentConfig().getOpenaiCompatible().get(LiveTestSupport.COMPATIBLE_CONFIG_KEY);
        String modelName = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName)
                .generateOptions(GenerateOptions.builder().temperature(0.1).maxTokens(64).build());
        if (cred.getBaseUrl() != null && !cred.getBaseUrl().isBlank()) {
            builder.baseUrl(cred.getBaseUrl());
        }
        return builder.build();
    }
}

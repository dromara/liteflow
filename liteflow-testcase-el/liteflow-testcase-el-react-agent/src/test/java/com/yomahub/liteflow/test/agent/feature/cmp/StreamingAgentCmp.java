package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

/**
 * 显式开启底层模型 stream 模式的 Agent，配合 ExecuteOption.eventListener 验证流式事件。
 */
@Component("streamingAgent")
public class StreamingAgentCmp extends AbstractCompatibleCustomAgentCmp {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(128)
                .stream(true);
    }
}

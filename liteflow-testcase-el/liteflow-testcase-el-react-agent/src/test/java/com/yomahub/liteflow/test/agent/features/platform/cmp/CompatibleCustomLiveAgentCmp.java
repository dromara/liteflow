package com.yomahub.liteflow.test.agent.features.platform.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 真实 compatible-custom Agent。该组件不覆写 buildModel()，会真正使用 OpenAI 兼容端点。
 */
@Component("compatibleCustomLiveAgent")
public class CompatibleCustomLiveAgentCmp extends ReActAgentComponent {

    @Value("${test.compatible-custom.model:gpt-4o-mini}")
    private String modelName;

    @Override
    protected ModelSpec<?> model() {
        return OpenAICompatible.custom(
                        ReActAgentFeatureTestSupport.COMPATIBLE_CONFIG_KEY,
                        modelName)
                .temperature(0.1)
                .maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的连通性测试助手，只能用一句中文回复。";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }
}

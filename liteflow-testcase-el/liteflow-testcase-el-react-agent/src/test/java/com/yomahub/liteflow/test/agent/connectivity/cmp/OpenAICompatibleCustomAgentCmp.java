package com.yomahub.liteflow.test.agent.connectivity.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

/**
 * 自定义 OpenAI 兼容厂商连通性测试组件。复用同一套
 * {@code liteflow.agent.openai-compatible.compatible-custom.*} 配置，
 * 由测试用户在环境变量中提供 baseUrl + apiKey + model。
 */
@Component("openaiCompatibleCustomAgent")
public class OpenAICompatibleCustomAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 OpenAI 兼容连通性测试助手，请用一句简短中文作答。";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 2;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }
}

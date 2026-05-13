package com.yomahub.liteflow.test.agent.connectivity.cmp;

import com.yomahub.liteflow.agent.anthropic.AnthropicCompatible;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import org.springframework.stereotype.Component;

/**
 * AnthropicCompatible 网关连通性测试组件。configKey 固定为 {@code gateway}，
 * baseUrl/apiKey/model 由环境变量提供。
 */
@Component("anthropicCompatibleAgent")
public class AnthropicCompatibleAgentCmp extends ReActAgentComponent {

    public static final String GATEWAY_CONFIG_KEY = "gateway";

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.ANTHROPIC_GATEWAY_MODEL, "claude-3-5-haiku-latest");
        return AnthropicCompatible.custom(GATEWAY_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 Anthropic 兼容网关连通性测试助手，请用一句简短中文作答。";
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

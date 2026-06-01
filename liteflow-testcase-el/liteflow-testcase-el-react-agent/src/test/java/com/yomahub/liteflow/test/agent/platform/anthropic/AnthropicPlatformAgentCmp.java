package com.yomahub.liteflow.test.agent.platform.anthropic;

import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import org.springframework.stereotype.Component;

/**
 * Anthropic 头等平台连通性测试组件。
 */
@Component("anthropicPlatformAgent")
public class AnthropicPlatformAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.ANTHROPIC_MODEL, "claude-3-5-haiku-latest");
        return Anthropic.of(model).temperature(0.1).maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 Anthropic 平台连通性测试助手，请用一句简短中文作答。";
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

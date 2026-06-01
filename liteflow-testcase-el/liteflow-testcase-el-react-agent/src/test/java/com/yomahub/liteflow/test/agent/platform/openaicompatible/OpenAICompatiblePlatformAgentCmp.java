package com.yomahub.liteflow.test.agent.platform.openaicompatible;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.springframework.stereotype.Component;

/**
 * OpenAICompatible.custom 平台连通性测试组件（自定义 baseUrl + apiKey）。
 */
@Component("openaicompatiblePlatformAgent")
public class OpenAICompatiblePlatformAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 OpenAI 兼容平台连通性测试助手，请用一句简短中文作答。";
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

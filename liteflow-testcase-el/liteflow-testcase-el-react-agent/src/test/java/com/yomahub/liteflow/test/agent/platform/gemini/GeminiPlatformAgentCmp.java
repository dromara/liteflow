package com.yomahub.liteflow.test.agent.platform.gemini;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import org.springframework.stereotype.Component;

/**
 * Gemini 头等平台连通性测试组件。
 */
@Component("geminiPlatformAgent")
public class GeminiPlatformAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.GEMINI_MODEL, "gemini-2.5-flash");
        return Gemini.of(model).temperature(0.1).maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 Gemini 平台连通性测试助手，请用一句简短中文作答。";
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

package com.yomahub.liteflow.test.agent.platform.deepseek;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import org.springframework.stereotype.Component;

/**
 * DeepSeek（OpenAI 兼容预设，内置默认 baseUrl）连通性测试组件。
 */
@Component("deepseekPlatformAgent")
public class DeepSeekPlatformAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.DEEPSEEK_MODEL, "deepseek-chat");
        return DeepSeek.of(model).temperature(0.1).maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 DeepSeek 连通性测试助手，请用一句简短中文作答。";
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

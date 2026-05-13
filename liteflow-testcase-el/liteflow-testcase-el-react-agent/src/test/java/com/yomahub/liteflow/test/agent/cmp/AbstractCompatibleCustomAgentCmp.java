package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;

/**
 * 所有功能测试用的 Agent 父类。
 *
 * <p>统一通过 {@link OpenAICompatible#custom(String, String)} 解析
 * {@code liteflow.agent.openai-compatible.compatible-custom.*} 凭据。
 * 真实 baseUrl/apiKey/model 由调用方在 @BeforeEach 中通过
 * {@link LiveTestSupport#ensureCompatibleCustomCredentialOrSkip} 装入；
 * 子类只需要覆写 systemPrompt 和必要的开关，不需要再操心模型构造。
 *
 * <p>默认关闭 Shell 与 workspace 文件工具，避免普通用例触发不必要的副作用；
 * 需要这些工具的测试在子类覆写 {@code enableShellTool/enableWorkspaceFileTools}。
 */
public abstract class AbstractCompatibleCustomAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(128);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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

    @Override
    protected boolean enableReActLogging() {
        return false;
    }
}

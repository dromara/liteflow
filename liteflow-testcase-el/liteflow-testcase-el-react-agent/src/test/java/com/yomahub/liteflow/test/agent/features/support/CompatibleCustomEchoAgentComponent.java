package com.yomahub.liteflow.test.agent.features.support;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import io.agentscope.core.model.Model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 功能测试使用的 OpenAI 兼容模型测试桩。
 *
 * <p>组件仍然通过 {@link OpenAICompatible#custom(String, String)} 解析
 * {@code liteflow.agent.openai-compatible.compatible-custom.*} 配置，覆盖真实入口的
 * credential 读取路径；真正执行时返回本地 Echo 模型，避免普通功能测试依赖外网。
 */
public abstract class CompatibleCustomEchoAgentComponent extends ReActAgentComponent {

    public static final AtomicInteger COMPATIBLE_SPEC_RESOLVE_COUNT = new AtomicInteger();

    public static void resetCompatibleProbe() {
        COMPATIBLE_SPEC_RESOLVE_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        return OpenAICompatible.custom(
                        ReActAgentFeatureTestSupport.COMPATIBLE_CONFIG_KEY,
                        "compatible-custom-test-model")
                .temperature(0.1)
                .maxTokens(64)
                .stream(false);
    }

    @Override
    protected Model buildModel() {
        model().resolve(agentConfig());
        COMPATIBLE_SPEC_RESOLVE_COUNT.incrementAndGet();
        return new CompatibleCustomEchoModel(getNodeId());
    }

    @Override
    protected String systemPrompt() {
        return "compatible custom test agent: " + getNodeId();
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
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

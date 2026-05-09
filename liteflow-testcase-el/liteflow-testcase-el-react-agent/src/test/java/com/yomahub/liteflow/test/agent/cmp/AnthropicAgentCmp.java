package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Anthropic 平台连通性测试组件。
 *
 * <p>该组件只用于真实平台冒烟测试。测试类会在 API Key 缺失时跳过，
 * 避免普通单元测试依赖外部网络和真实账号。
 */
@Component("anthropicAgent")
public class AnthropicAgentCmp extends ReActAgentComponent {

    @Value("${test.anthropic.model:claude-3-5-haiku-latest}")
    private String modelName;

    @Override
    protected ModelSpec<?> model() {
        return Anthropic.of(modelName);
    }

    @Override
    protected String systemPrompt() {
        return "你是 Anthropic 连通性测试助手，只需要用一句中文简短回答。";
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
}

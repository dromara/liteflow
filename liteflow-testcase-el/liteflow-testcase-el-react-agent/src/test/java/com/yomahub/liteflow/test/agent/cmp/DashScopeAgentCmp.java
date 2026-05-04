package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DashScope 平台连通性测试组件。
 *
 * <p>该组件只用于真实平台冒烟测试，验证 DashScope provider 可以通过
 * LiteFlow EL 节点完成一次完整调用。
 */
@Component("dashScopeAgent")
public class DashScopeAgentCmp extends ReActAgentComponent {

    @Value("${test.dashscope.model:qwen-plus}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DashScope.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是 DashScope 连通性测试助手，只需要用一句中文简短回答。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        Object reqData = ctx.getSlot().getChainReqData(ctx.getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }
}

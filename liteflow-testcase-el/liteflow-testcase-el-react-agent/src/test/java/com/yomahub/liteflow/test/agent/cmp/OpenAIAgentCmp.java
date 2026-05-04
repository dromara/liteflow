package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI 平台连通性测试组件。
 *
 * <p>该组件只用于真实平台冒烟测试：当测试环境提供 OpenAI API Key 时，
 * LiteFlow 会通过 {@code THEN(prepare, openAIAgent, recordReply)} 调用一次模型。
 */
@Component("openAIAgent")
public class OpenAIAgentCmp extends ReActAgentComponent {

    @Value("${test.openai.model:gpt-4.1-mini}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return OpenAI.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是 OpenAI 连通性测试助手，只需要用一句中文简短回答。";
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

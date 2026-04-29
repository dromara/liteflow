package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DeepSeek Agent（OpenAI 兼容协议）。从 application.properties 读模型名，
 * apiKey 由 liteflow.agent.openai-compatible.deepseek.api-key 注入到 AgentConfig。
 */
@Component("deepseekAgent")
public class DeepSeekAgentCmp extends ReActAgentComponent {

    @Value("${test.deepseek.model:deepseek-chat}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DeepSeek.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是一名简洁的中文助理，回答严格控制在两句话以内。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

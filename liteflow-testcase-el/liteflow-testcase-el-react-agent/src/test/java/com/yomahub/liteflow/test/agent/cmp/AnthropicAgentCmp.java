package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.anthropic.Anthropic;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("anthropicAgent")
public class AnthropicAgentCmp extends ReActAgentComponent {

    @Value("${test.anthropic.model:claude-sonnet-4-5}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return Anthropic.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是一名严谨的技术作者，回答简短而准确。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

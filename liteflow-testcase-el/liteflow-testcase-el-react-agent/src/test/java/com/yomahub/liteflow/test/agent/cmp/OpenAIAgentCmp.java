package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("openaiAgent")
public class OpenAIAgentCmp extends ReActAgentComponent {

    @Value("${test.openai.model:gpt-4o-mini}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return OpenAI.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "You are a concise assistant. Answer in Chinese, max two sentences.";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

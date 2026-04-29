package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("geminiAgent")
public class GeminiAgentCmp extends ReActAgentComponent {

    @Value("${test.gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return Gemini.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "You are a concise assistant. Answer in Chinese, one sentence.";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

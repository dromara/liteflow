package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.dashscope.DashScope;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("dashscopeAgent")
public class DashScopeAgentCmp extends ReActAgentComponent {

    @Value("${test.dashscope.model:qwen-plus}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DashScope.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是阿里云通义千问助手，请用简短中文回答。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

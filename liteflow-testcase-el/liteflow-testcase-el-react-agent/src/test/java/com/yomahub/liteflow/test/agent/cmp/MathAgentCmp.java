package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.test.agent.tool.CalculatorTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数学 Agent：复用 DeepSeek 后端，但额外注入 {@link CalculatorTool}。
 * 演示通过覆写 tools() 把自定义 @Tool 对象交给 ReAct 推理。
 */
@Component("mathAgent")
public class MathAgentCmp extends ReActAgentComponent {

    @Value("${test.deepseek.model:deepseek-chat}")
    private String modelName;

    @Override
    protected ModelSpec<?> model(ReActAgentContext ctx) {
        return DeepSeek.of(modelName);
    }

    @Override
    protected String systemPrompt(ReActAgentContext ctx) {
        return "你是一名计算助理，凡是涉及算术，必须调用 calculator 工具，不要心算。";
    }

    @Override
    protected String userPrompt(ReActAgentContext ctx) {
        return String.valueOf(ctx.getSlot().getChainReqData(ctx.getSlot().getChainId()));
    }

    @Override
    protected List<Object> tools(ReActAgentContext ctx) {
        return List.of(new CalculatorTool());
    }

    @Override protected boolean enableShellTool() { return false; }
    @Override protected boolean enableWorkspaceFileTools() { return false; }
}

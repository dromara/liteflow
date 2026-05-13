package com.yomahub.liteflow.test.agent.connectivity.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.Minimax;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import org.springframework.stereotype.Component;

@Component("minimaxAgent")
public class MinimaxConnectivityAgentCmp extends ReActAgentComponent {

    @Override
    protected ModelSpec<?> model() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.MINIMAX_MODEL, "MiniMax-Text-01");
        return Minimax.of(model).temperature(0.1).maxTokens(64);
    }

    @Override
    protected String systemPrompt() {
        return "你是 Minimax 连通性测试助手，请用一句简短中文作答。";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 2;
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

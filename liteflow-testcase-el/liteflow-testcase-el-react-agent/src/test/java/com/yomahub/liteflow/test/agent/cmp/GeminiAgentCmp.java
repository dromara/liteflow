package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.gemini.Gemini;
import com.yomahub.liteflow.agent.model.ModelSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("geminiAgent")
public class GeminiAgentCmp extends ReActAgentComponent {

    @Value("${test.gemini.model:gemini-3-flash-preview}")
    private String modelName;

    @Override
    protected ModelSpec<?> model() {
        return Gemini.of(modelName);
    }

    @Override
    protected String systemPrompt() {
        return "你是一个助手，需要回答我的问题，你可以执行 shell 来获得必要的答案";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}

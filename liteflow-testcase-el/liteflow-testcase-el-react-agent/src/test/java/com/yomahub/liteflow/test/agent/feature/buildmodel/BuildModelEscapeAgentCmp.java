package com.yomahub.liteflow.test.agent.feature.buildmodel;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAIModelFactory;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示 guide §4.6 buildModel() 逃生舱：完全自行构造 agentscope Model。
 * 这里用 OpenAI 兼容自定义凭据构造一个真实 OpenAIChatModel，验证覆写 buildModel()
 * 后 {@code model().resolve(...)} 不再被调用，但仍能完成真实模型调用。
 */
@Component("buildModelEscapeAgent")
public class BuildModelEscapeAgentCmp extends ReActAgentComponent {

    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();

    public static void reset() {
        BUILD_MODEL_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        // model() 仍是抽象，必须实现；但 buildModel() 覆写后 resolve 不会被调用。
        return LiveTestSupport.compatibleCustomModel();
    }

    @Override
    protected Model buildModel() {
        BUILD_MODEL_COUNT.incrementAndGet();
        PlatformCredential cred = agentConfig().getOpenaiCompatible().get(LiveTestSupport.COMPATIBLE_CONFIG_KEY);
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAIModelFactory.custom(cred.getApiKey(), cred.getBaseUrl(), model);
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt() {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
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

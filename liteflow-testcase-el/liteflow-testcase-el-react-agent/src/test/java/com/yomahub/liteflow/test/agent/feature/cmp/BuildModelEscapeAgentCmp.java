package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.support.FakeEchoModel;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示 guide §4.6 buildModel() 逃生舱：完全自行构造 agentscope Model。
 * 这里返回本地 fake model，避免框架语义测试依赖外部模型服务。
 */
@Component("buildModelEscapeAgent")
public class BuildModelEscapeAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();

    public static void reset() {
        BUILD_MODEL_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        // model() 仍是抽象，必须实现；但 buildModel() 覆写后 resolve 不会被调用。
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, "unused-model-spec");
    }

    @Override
    protected Model buildModel() {
        BUILD_MODEL_COUNT.incrementAndGet();
        return new FakeEchoModel("fake-build-model-escape");
    }
}

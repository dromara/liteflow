package com.yomahub.liteflow.test.agent.real;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.OpenAICompatible;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestEnv;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.BeforeEach;

/**
 * 真实模型（OpenAI 兼容自定义端点）live 测试公共基类。
 *
 * <p>凭据来自 env.txt（或环境变量 / -D 参数），缺失任一项时整个测试类跳过。
 * 所有 real 包测试统一使用 {@link LiveTestSupport#COMPATIBLE_CONFIG_KEY} 配置段。
 */
public abstract class RealAgentTestBase extends BaseAgentLiveTest {

    @BeforeEach
    public void installRealCredential() {
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, getClass().getSimpleName());
    }

    /** 真实模型描述符：OpenAICompatible.custom，maxTokens 放宽以容纳工具调用轮次。 */
    public static ModelSpec<?> realModel() {
        String model = LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
        return OpenAICompatible.custom(LiveTestSupport.COMPATIBLE_CONFIG_KEY, model)
                .temperature(0.1)
                .maxTokens(1024);
    }

    /** 真实模型名（供断言与日志）。 */
    public static String realModelName() {
        return LiveTestEnv.resolveOrDefault(LiveTestEnv.COMPATIBLE_MODEL, "gpt-4o-mini");
    }
}

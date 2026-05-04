package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 测试 ReActAgentComponent 暴露的生命周期扩展点。
 *
 * <p>这里重点覆盖 maxIterations 覆写和 hooks(ctx) 注册。两者都发生在 agent 构建阶段，
 * 因此测试通过 Hook 观察真正构建出来的 ReActAgent，而不是只断言组件字段。
 */
public class ReActAgentLifecycleTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证组件覆写 maxIterations 后，实际 ReActAgent 使用的是组件级配置。
     */
    @Test
    public void testStubAgentUsesOverriddenMaxIterationsAndHooks() {
        StubReActAgentCmp.overriddenMaxIterations = 3;

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "max-iterations");

        Assertions.assertTrue(response.isSuccess());

        // Hook 中读取到的 maxIters 为 3，说明组件覆写值已经进入 ReActAgent 构建结果。
        Assertions.assertTrue(StubReActAgentCmp.MAX_ITERATIONS_SEEN.contains(3));

        // Hook 计数大于 0，说明 hooks(ctx) 注册的生命周期观察器确实被 ReAct 调用。
        Assertions.assertTrue(StubReActAgentCmp.HOOK_EVENT_COUNT.get() > 0,
                "custom hooks should observe ReAct lifecycle events");
    }
}

package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 测试 ReAct Agent 的 Session 管理行为。
 *
 * <p>测试桩组件固定返回同一个 conversationId，因此同一个测试方法内连续执行两次链路时，
 * 第二次应该复用第一次构建好的 ReActAgent 实例，而不是重新构建模型、工具和系统提示词。
 */
public class ReActAgentSessionTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证相同 conversationId + 同一 agent 节点会复用受管 ReActAgent 实例。
     */
    @Test
    public void testStubAgentReusesManagedSessionForSameConversationId() {
        LiteflowResponse first = flowExecutor.execute2Resp("stubAgentChain", "first");
        LiteflowResponse second = flowExecutor.execute2Resp("stubAgentChain", "second");

        Assertions.assertTrue(first.isSuccess());
        Assertions.assertTrue(second.isSuccess());

        // 同一个 conversationId 在同一 agent 上只会触发一次模型解析，说明第二次执行复用了已有 agent。
        Assertions.assertEquals(1, StubReActAgentCmp.SPEC_RESOLVE_COUNT.get(),
                "same conversation id + agent key should reuse built ReActAgent");

        // 系统提示词只在 agent 构建阶段使用一次，用户提示词则每次执行都要重新生成。
        Assertions.assertEquals(1, StubReActAgentCmp.SYSTEM_PROMPT_COUNT.get(),
                "system prompt is only needed when the ReActAgent is built");
        Assertions.assertEquals(2, StubReActAgentCmp.USER_PROMPT_COUNT.get());

        // 模型桩内部的调用次数递增，进一步证明两次调用落在同一个模型实例上。
        Assertions.assertEquals(2, StubReActAgentCmp.MODEL_PROBES.size());
        Assertions.assertEquals(1, StubReActAgentCmp.MODEL_PROBES.get(0).callCount());
        Assertions.assertEquals(2, StubReActAgentCmp.MODEL_PROBES.get(1).callCount());

        // 第二次执行仍然能读取新的 LiteFlow 请求数据，而不是复用第一次的用户输入。
        Assertions.assertTrue(StubReActAgentCmp.MODEL_PROBES.get(1).inputTexts().contains("second"));

        // LiteflowResponse 应该能透出 chain 内解析到的 conversationId。
        Assertions.assertEquals(StubReActAgentCmp.FIXED_CONVERSATION_ID, first.getConversationId());
    }
}

package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 测试 ReAct Agent 是否能作为普通 LiteFlow 节点参与 EL 编排。
 *
 * <p>这个类只验证最基础的 EL 集成：规则文件中使用
 * {@code THEN(prepare, stubAgent, recordReply)} 三个节点，前置节点写入请求数据，
 * agent 节点读取上下文并生成回复，后置节点再把回复记录到 slot 输出中。
 */
public class ReActAgentELChainTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证一个 ReActAgentComponent 可以被 THEN 串行编排执行。
     */
    @Test
    public void testStubAgentRunsInsideThreeNodeThenChain() {
        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "hello-liteflow-agent");

        // 链路必须完整成功，说明 prepare、stubAgent、recordReply 三个节点都已执行。
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // recordReply 会把 agent 回复保存到本节点 output，测试由此确认后置节点拿到了回复。
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply);
        Assertions.assertTrue(reply.toString().contains("reply:" + StubReActAgentCmp.FIXED_CONVERSATION_ID));

        // userPrompt 必须来自 prepare 写入的 chainReqData，不能绕开 LiteFlow slot 上下文。
        Assertions.assertEquals(List.of("hello-liteflow-agent"), StubReActAgentCmp.USER_PROMPTS);

        // 首次执行会构建一次模型和系统提示词，并调用一次默认回复处理逻辑。
        Assertions.assertEquals(1, StubReActAgentCmp.SPEC_RESOLVE_COUNT.get());
        Assertions.assertEquals(1, StubReActAgentCmp.SYSTEM_PROMPT_COUNT.get());
        Assertions.assertEquals(1, StubReActAgentCmp.HANDLE_REPLY_COUNT.get());
    }
}

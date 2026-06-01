package com.yomahub.liteflow.test.agent.feature.basicchain;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;

/**
 * 验证最基础的 THEN 链路集成：basicAgent → recordReply。
 *
 * <p>对应 guide §2.4 / §2.5：Agent 作为普通 LiteFlow 节点，回复默认通过
 * slot.responseData 流转给下游节点；ctx() 在 process() 生命周期内可用，
 * conversationId 写回 slot 后通过 LiteflowResponse 暴露给调用方。
 */
@TestPropertySource("classpath:/feature/basicchain/application.properties")
@SpringBootTest(classes = BasicChainTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.basicchain")
public class BasicChainTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetProbe() {
        BasicAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "BasicChainTest");
    }

    @Test
    public void testBasicChainPropagatesPromptThroughAgent() {
        String prompt = "请用一句话介绍 LiteFlow ReAct Agent。";
        LiteflowResponse response = flowExecutor.execute2Resp("basicAgentChain", prompt);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 回复已经通过默认 handleReply 写入 responseData，并被 recordReply 复制到 output。
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply, "agent reply must reach recordReply node");
        Assertions.assertFalse(reply.toString().isBlank(), "agent reply must not be blank");

        // userPrompt/systemPrompt 各被调用一次，handleReply 也调用一次。
        Assertions.assertEquals(1, BasicAgentCmp.SYSTEM_PROMPT_COUNT.get());
        Assertions.assertEquals(1, BasicAgentCmp.USER_PROMPT_COUNT.get());
        Assertions.assertEquals(1, BasicAgentCmp.HANDLE_REPLY_COUNT.get());

        // ctx() 在 process() 内可用，能拿到非空 conversationId / agentKey / workspace。
        Assertions.assertNotNull(BasicAgentCmp.SEEN_CONVERSATION_ID.get());
        Assertions.assertEquals("basicAgent", BasicAgentCmp.SEEN_AGENT_KEY.get());
        Assertions.assertNotNull(BasicAgentCmp.SEEN_WORKSPACE.get());
        Assertions.assertTrue(Files.isDirectory(BasicAgentCmp.SEEN_WORKSPACE.get()),
                "workspace dir must exist on disk");

        // LiteflowResponse 透出 chain 内解析到的 conversationId。
        Assertions.assertEquals(BasicAgentCmp.SEEN_CONVERSATION_ID.get(), response.getConversationId());

        // AgentProbe 透露：reasoning 至少触发一次（真实模型已经回复）。
        Assertions.assertTrue(BasicAgentCmp.PROBE.get().reasoningCount() > 0,
                "real LLM should emit at least one reasoning event");
        Assertions.assertNotNull(BasicAgentCmp.PROBE.get().observedAgentId());
        // 当组件关闭 shell/workspace 工具时，Toolkit 应该不包含这些内置工具。
        Assertions.assertFalse(BasicAgentCmp.PROBE.get().toolNames().contains("execute_shell_command"));
        Assertions.assertFalse(BasicAgentCmp.PROBE.get().toolNames().contains("read_file"));
    }
}

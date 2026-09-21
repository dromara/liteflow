package com.yomahub.liteflow.test.agent.feature.chatusage;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentTest;
import io.agentscope.core.model.ChatUsage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 覆盖 guide §3 中 {@code ctx().getChatUsage()} 的 token 用量累计语义。
 *
 * <p>进程内确定性模型固定上报 ChatUsage，因此这里可以验证精确累计值。
 */
@TestPropertySource("classpath:/feature/chatusage/application.properties")
@SpringBootTest(classes = ChatUsageTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.chatusage")
public class ChatUsageTest extends BaseAgentTest {

    @BeforeEach
    public void reset() {
        ChatUsageAgentCmp.reset();
    }

    @Test
    public void testChatUsageIsReadableInHandleReply() {
        LiteflowResponse response = flowExecutor.execute2Resp("chatUsageChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 回复经默认 handleReply 写入 responseData。
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply);
        Assertions.assertFalse(reply.toString().isBlank());

        // getChatUsage() 在 handleReply 生命周期内可调用。
        Assertions.assertTrue(ChatUsageAgentCmp.GET_USAGE_CALLED.get(),
                "handleReply 中应能调用 ctx().getChatUsage()");

        ChatUsage usage = ChatUsageAgentCmp.CAPTURED.get();
        Assertions.assertNotNull(usage);
        Assertions.assertEquals(7, usage.getInputTokens());
        Assertions.assertEquals(3, usage.getOutputTokens());
        Assertions.assertEquals(10, usage.getTotalTokens());
    }
}

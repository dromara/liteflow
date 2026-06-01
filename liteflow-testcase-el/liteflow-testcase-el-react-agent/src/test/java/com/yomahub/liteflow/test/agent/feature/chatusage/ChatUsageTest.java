package com.yomahub.liteflow.test.agent.feature.chatusage;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
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
 * <p>真实模型/网关大多会在响应里上报 ChatUsage；部分网关可能丢失 usage（返回 null）。
 * 因此这里断言：链路成功、getChatUsage 在 handleReply 中可调用；当 usage 存在时 token 数自洽。
 */
@TestPropertySource("classpath:/feature/chatusage/application.properties")
@SpringBootTest(classes = ChatUsageTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.chatusage")
public class ChatUsageTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        ChatUsageAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "ChatUsageTest");
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

        // 当模型/网关上报了 usage 时，token 数应自洽。
        ChatUsage usage = ChatUsageAgentCmp.CAPTURED.get();
        if (usage != null) {
            Assertions.assertTrue(usage.getTotalTokens() > 0,
                    "上报 usage 时 totalTokens 应为正：" + usage.getTotalTokens());
            Assertions.assertTrue(usage.getInputTokens() > 0,
                    "上报 usage 时 inputTokens 应为正：" + usage.getInputTokens());
            Assertions.assertTrue(usage.getOutputTokens() >= 0,
                    "上报 usage 时 outputTokens 应非负：" + usage.getOutputTokens());
        }
    }
}

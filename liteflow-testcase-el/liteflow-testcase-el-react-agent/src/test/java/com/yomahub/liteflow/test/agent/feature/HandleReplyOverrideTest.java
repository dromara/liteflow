package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import com.yomahub.liteflow.test.agent.feature.cmp.CustomHandleReplyAgentCmp;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 覆盖 guide §2.5 中 handleReply 的两种语义：
 * <ul>
 *   <li>默认 handleReply 写入 slot.responseData（其他测试已覆盖）；</li>
 *   <li>覆写后写入 slot.setOutput(nodeId, ...) 以避免被后续 Agent 覆盖。</li>
 * </ul>
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = HandleReplyOverrideTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class HandleReplyOverrideTest extends BaseAgentLiveTest {

    @BeforeEach
    public void ensureCredential() {
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "HandleReplyOverrideTest");
    }

    @Test
    public void testHandleReplyOverrideWritesToOutputInsteadOfResponseData() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "customHandleReplyChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 自定义 handleReply 没有写 responseData，因此 recordReply 也拿不到 reply。
        Assertions.assertNull(response.getSlot().getResponseData(),
                "custom handleReply should not write slot.responseData");
        Assertions.assertNull(response.getSlot().getOutput(RecordReplyCmp.NODE_ID),
                "recordReply should see no reply when handleReply was customized");

        // 自定义 handleReply 用 nodeId 作 key 写入 slot.output。
        Object reply = response.getSlot().getOutput(CustomHandleReplyAgentCmp.OUTPUT_KEY);
        Assertions.assertNotNull(reply, "custom handleReply must write reply to slot.output[nodeId]");
        Assertions.assertFalse(reply.toString().isBlank(), "reply text must not be blank");
    }
}

package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.BasicAgentCmp;
import com.yomahub.liteflow.test.agent.feature.cmp.ResolveCidAgentCmp;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

/**
 * 覆盖 guide §5.2 中 conversationId 解析的多条路径：
 * <ul>
 *   <li>{@code ExecuteOption.conversationId(cid)} 显式传入；</li>
 *   <li>{@code ExecuteOption.autoConversationId()} 自动生成；</li>
 *   <li>请求 Map 中的 {@code conversationId} 字段；</li>
 *   <li>组件覆写 {@code resolveConversationId()} 拼接业务 cid；</li>
 *   <li>特殊字符通过 safeId 安全化。</li>
 * </ul>
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = ConversationIdTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class ConversationIdTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        BasicAgentCmp.reset();
        ResolveCidAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "ConversationIdTest");
    }

    @Test
    public void testExplicitExecuteOptionConversationIdIsHonored() {
        String cid = "explicit-cid-001";
        LiteflowResponse response = flowExecutor.execute2Resp("basicAgentChain", "go",
                ExecuteOption.of().conversationId(cid));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(cid, response.getConversationId());
        Assertions.assertEquals(cid, BasicAgentCmp.SEEN_CONVERSATION_ID.get(),
                "组件 ctx() 中的 conversationId 应该来自 ExecuteOption");
    }

    @Test
    public void testAutoConversationIdGeneratesNanoIdLikeValue() {
        LiteflowResponse response = flowExecutor.execute2Resp("basicAgentChain", "go",
                ExecuteOption.of().autoConversationId());

        Assertions.assertTrue(response.isSuccess());
        String cid = response.getConversationId();
        Assertions.assertNotNull(cid, "autoConversationId() 应生成非空 cid");
        Assertions.assertFalse(cid.isBlank());
        Assertions.assertEquals(cid, BasicAgentCmp.SEEN_CONVERSATION_ID.get());
    }

    @Test
    public void testConversationIdFromRequestDataMap() {
        String cid = "from-req-map-cid";
        LiteflowResponse response = flowExecutor.execute2Resp("basicAgentChain", Map.of(
                ReActAgentComponent.CONVERSATION_ID_REQUEST_KEY, cid));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(cid, response.getConversationId());
        Assertions.assertEquals(cid, BasicAgentCmp.SEEN_CONVERSATION_ID.get());
    }

    @Test
    public void testCustomResolveConversationIdComposedFromRequestObject() {
        LiteflowResponse response = flowExecutor.execute2Resp("resolveCidChain", Map.of(
                "userId", 42,
                "convId", "abc",
                "prompt", "请用一句话作答。"));

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals("user-42-conv-abc", ResolveCidAgentCmp.SEEN_CID.get(),
                "覆写 resolveConversationId 应基于请求体生成稳定业务 cid");
        Assertions.assertEquals("user-42-conv-abc", response.getConversationId());
    }

    @Test
    public void testConversationIdWithUnsafeCharsIsSanitizedForCtx() {
        String raw = "chat/user 1";
        LiteflowResponse response = flowExecutor.execute2Resp("basicAgentChain", "go",
                ExecuteOption.of().conversationId(raw));

        Assertions.assertTrue(response.isSuccess());
        // ExecuteOption 把原始 cid 写入 slot，LiteflowResponse 暴露的是原始值；
        // 但 ctx() 拿到的应该是 safeId 处理后的目录安全形式。
        Assertions.assertEquals(raw, response.getConversationId());
        Assertions.assertNotEquals(raw, BasicAgentCmp.SEEN_CONVERSATION_ID.get(),
                "含特殊字符的 cid 在 ctx 中应被 safeId 处理为目录安全格式");
        Assertions.assertFalse(BasicAgentCmp.SEEN_CONVERSATION_ID.get().contains("/"));
        Assertions.assertFalse(BasicAgentCmp.SEEN_CONVERSATION_ID.get().contains(" "));
    }
}

package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.CustomAgentKeyAgentCmp;
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
 * 覆盖 guide §5.3：组件覆写 {@code agentKey()} 后，ctx 中应反映新的 key，
 * 进而决定 SessionManager 缓存条目的隔离。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = AgentKeyOverrideTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class AgentKeyOverrideTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        CustomAgentKeyAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "AgentKeyOverrideTest");
    }

    @Test
    public void testCustomAgentKeyReplacesNodeIdDefault() {
        CustomAgentKeyAgentCmp.overriddenKey = "tenant-A__user-007";

        LiteflowResponse response = flowExecutor.execute2Resp(
                "customAgentKeyChain", "go",
                ExecuteOption.of().conversationId("ak-cid"));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ak-cid", CustomAgentKeyAgentCmp.SEEN_CID.get());
        Assertions.assertEquals("tenant-A__user-007", CustomAgentKeyAgentCmp.SEEN_AGENT_KEY.get(),
                "ctx().getAgentKey() 应反映组件覆写值，而不是 nodeId");
    }

    @Test
    public void testUnsafeAgentKeyIsSanitized() {
        CustomAgentKeyAgentCmp.overriddenKey = "team/dev session#1";

        LiteflowResponse response = flowExecutor.execute2Resp(
                "customAgentKeyChain", "go",
                ExecuteOption.of().conversationId("ak-cid-2"));

        Assertions.assertTrue(response.isSuccess());
        String seen = CustomAgentKeyAgentCmp.SEEN_AGENT_KEY.get();
        Assertions.assertNotEquals("team/dev session#1", seen,
                "含特殊字符的 agentKey 应被 safeId 处理为目录安全格式");
        Assertions.assertFalse(seen.contains("/"));
        Assertions.assertFalse(seen.contains(" "));
        Assertions.assertFalse(seen.contains("#"));
    }
}

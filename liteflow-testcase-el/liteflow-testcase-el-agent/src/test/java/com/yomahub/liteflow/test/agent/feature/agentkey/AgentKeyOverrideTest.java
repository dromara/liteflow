package com.yomahub.liteflow.test.agent.feature.agentkey;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * 覆盖 guide §5.3：组件覆写 {@code agentKey()} 后，ctx 中应反映新的 key，
 * 进而决定 AgentScope 2 state namespace 的隔离；目录越界的 key 必须被拒绝。
 */
@TestPropertySource("classpath:/feature/agentkey/application.properties")
@SpringBootTest(classes = AgentKeyOverrideTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.agentkey")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AgentKeyOverrideTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        CustomAgentKeyAgentCmp.reset();
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
    public void testUnsafeAgentKeyIsRejectedAndDoesNotPoisonTheNextInvocation() {
        CustomAgentKeyAgentCmp.overriddenKey = "team/dev session#1";

        LiteflowResponse response = flowExecutor.execute2Resp(
                "customAgentKeyChain", "go",
                ExecuteOption.of().conversationId("ak-cid-2"));

        Assertions.assertFalse(response.isSuccess());
        Throwable cause = response.getCause();
        while (cause.getCause() != null) cause = cause.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);
        Assertions.assertTrue(cause.getMessage().contains("agentKey"));

        CustomAgentKeyAgentCmp.overriddenKey = "team-dev";
        LiteflowResponse retry = flowExecutor.execute2Resp("customAgentKeyChain", "go",
                ExecuteOption.of().conversationId("ak-cid-2"));
        Assertions.assertTrue(retry.isSuccess());
        Assertions.assertEquals("team-dev", CustomAgentKeyAgentCmp.SEEN_AGENT_KEY.get());
        Assertions.assertEquals("ak-cid-2", CustomAgentKeyAgentCmp.SEEN_RUNTIME_SESSION.get());
    }
}

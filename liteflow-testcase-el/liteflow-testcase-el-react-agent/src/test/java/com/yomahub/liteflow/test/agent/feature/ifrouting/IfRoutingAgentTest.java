package com.yomahub.liteflow.test.agent.feature.ifrouting;

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

import java.util.Map;

/**
 * 覆盖 guide §8.2：IF 路由 + Agent。布尔节点根据 request 字段决定走哪个 Agent，
 * 另一个分支不应被执行。
 */
@TestPropertySource("classpath:/feature/ifrouting/application.properties")
@SpringBootTest(classes = IfRoutingAgentTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.ifrouting")
public class IfRoutingAgentTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetCounts() {
        MathBranchAgentCmp.reset();
        DefaultBranchAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "IfRoutingAgentTest");
    }

    @Test
    public void testMathBranchIsTakenForMathRequest() {
        LiteflowResponse response = flowExecutor.execute2Resp("ifRoutingChain",
                Map.of("type", "math", "prompt", "1+1 等于几？"));

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(1, MathBranchAgentCmp.INVOCATION_COUNT.get(),
                "type=math 时应只触发 mathBranchAgent");
        Assertions.assertEquals(0, DefaultBranchAgentCmp.INVOCATION_COUNT.get(),
                "type=math 时不应触发 defaultBranchAgent");
        Assertions.assertNotNull(response.getSlot().getOutput("mathBranchAgent"));
    }

    @Test
    public void testDefaultBranchIsTakenForOtherRequest() {
        LiteflowResponse response = flowExecutor.execute2Resp("ifRoutingChain",
                Map.of("type", "chat", "prompt", "请简短自我介绍。"));

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(0, MathBranchAgentCmp.INVOCATION_COUNT.get());
        Assertions.assertEquals(1, DefaultBranchAgentCmp.INVOCATION_COUNT.get());
        Assertions.assertNotNull(response.getSlot().getOutput("defaultBranchAgent"));
    }
}

package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import com.yomahub.liteflow.test.agent.feature.cmp.BuildModelEscapeAgentCmp;
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
 * 覆盖 guide §4.6：覆写 {@code buildModel()} 时 {@code model().resolve(...)} 不会被调用，
 * 但仍能完成 ReActAgent 构造和真实模型调用。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = BuildModelEscapeTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class BuildModelEscapeTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        BuildModelEscapeAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "BuildModelEscapeTest");
    }

    @Test
    public void testBuildModelEscapeHatchWorks() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "buildModelEscapeChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(1, BuildModelEscapeAgentCmp.BUILD_MODEL_COUNT.get(),
                "覆写 buildModel() 应被调用一次");
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply);
        Assertions.assertFalse(reply.toString().isBlank());
    }
}

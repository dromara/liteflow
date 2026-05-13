package com.yomahub.liteflow.test.agent.features.compatiblecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * 覆盖 guide 中“自定义 OpenAI 兼容厂商”的基础接入方式。
 *
 * <p>测试链路固定为 {@code THEN(prepare, agent, record)}，确保 Agent 组件是作为
 * LiteFlow 节点参与整体 EL 编排，而不是脱离 LiteFlow 单独调用。
 */
@TestPropertySource(value = "classpath:/agent/features/compatiblecustom/application.properties")
@SpringBootTest(classes = CompatibleCustomFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.compatiblecustom.cmp" })
public class CompatibleCustomFeatureTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ReActAgentFeatureTestSupport.ensureAgentConfig(
                liteflowConfig,
                "target/wk_react_agent_compatiblecustom",
                false,
                null,
                ShellMode.DISABLED);
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
        CompatibleCustomEchoAgentComponent.resetCompatibleProbe();
    }

    @Test
    public void testCompatibleCustomAgentRunsInThenChain() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "compatibleCustomFeatureChain", "hello-compatible-custom");

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Assertions.assertEquals(1, CompatibleCustomEchoAgentComponent.COMPATIBLE_SPEC_RESOLVE_COUNT.get(),
                "首次构建 Agent 时应解析一次 compatible-custom ModelSpec");
        Assertions.assertTrue(response.getSlot().getOutput("compatibleCustomRecord").toString()
                .contains("hello-compatible-custom"));
    }
}

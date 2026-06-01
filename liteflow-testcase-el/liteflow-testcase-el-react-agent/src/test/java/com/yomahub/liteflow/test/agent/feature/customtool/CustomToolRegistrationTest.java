package com.yomahub.liteflow.test.agent.feature.customtool;

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

import java.util.Set;

/**
 * 验证 guide §8.1 中 {@code tools()} 注册的自定义工具会出现在 Agent Toolkit 中。
 *
 * <p>通过 AgentProbe 在 PreReasoning 阶段读取 toolkit 的 toolNames，不依赖模型是否真的调用工具。
 */
@TestPropertySource("classpath:/feature/customtool/application.properties")
@SpringBootTest(classes = CustomToolRegistrationTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.customtool")
public class CustomToolRegistrationTest extends BaseAgentLiveTest {

    @BeforeEach
    public void resetProbe() {
        CustomToolAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "CustomToolRegistrationTest");
    }

    @Test
    public void testCustomToolIsRegisteredInAgentToolkit() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "customToolChain", "请直接用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        Set<String> toolNames = CustomToolAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(toolNames.contains("echo_input"),
                "组件 tools() 注册的 echo_input 应出现在 toolkit：实际=" + toolNames);
        // shell / workspace 工具默认关闭，不应出现。
        Assertions.assertFalse(toolNames.contains("execute_shell_command"));
        Assertions.assertFalse(toolNames.contains("read_file"));
        // skills 未开启，也不应注册 load_skill_through_path。
        Assertions.assertFalse(toolNames.contains("load_skill_through_path"));
    }
}

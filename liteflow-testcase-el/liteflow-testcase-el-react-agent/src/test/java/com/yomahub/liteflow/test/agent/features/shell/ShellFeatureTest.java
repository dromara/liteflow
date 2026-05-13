package com.yomahub.liteflow.test.agent.features.shell;

import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
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
import java.util.List;

/**
 * 覆盖 guide 中受管 Shell 工具的安全配置。
 *
 * <p>配置为 WHITELIST，仅允许 {@code pwd}。测试验证命令在当前 conversation workspace
 * 中执行，并确认未列入白名单的命令会被拒绝。
 */
@TestPropertySource(value = "classpath:/agent/features/shell/application.properties")
@SpringBootTest(classes = ShellFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.shell.cmp" })
public class ShellFeatureTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ReActAgentFeatureTestSupport.ensureAgentConfig(
                liteflowConfig,
                "target/wk_react_agent_shell",
                false,
                null,
                ShellMode.WHITELIST);
        liteflowConfig.getAgent().getShell().setWhitelist(List.of("pwd"));
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
        CompatibleCustomEchoAgentComponent.resetCompatibleProbe();
        ShellFeatureProbe.reset();
    }

    @Test
    public void testManagedShellRunsInWorkspaceAndRejectsNonWhitelistedCommand() {
        LiteflowResponse response = flowExecutor.execute2Resp("shellFeatureChain", "shell-tools");

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Assertions.assertEquals(ShellFeatureProbe.WORKSPACE.get(), ShellFeatureProbe.PWD_OUTPUT.get().trim(),
                "pwd 应在当前 conversation workspace 目录下执行");
        Assertions.assertTrue(ShellFeatureProbe.DENIED_OUTPUT.get().contains("not allowed by whitelist"));
    }
}

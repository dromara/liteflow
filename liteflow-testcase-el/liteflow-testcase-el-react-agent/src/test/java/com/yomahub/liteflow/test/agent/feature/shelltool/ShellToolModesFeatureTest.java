package com.yomahub.liteflow.test.agent.feature.shelltool;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

/**
 * 覆盖 guide §6.4 受管 Shell 工具的三种模式：DISABLED / BLACKLIST / WHITELIST。
 * 每个测试都在同一条 THEN 链路中调用 Agent，但 BeforeEach 调整全局 shell.mode。
 */
@TestPropertySource("classpath:/feature/shelltool/application.properties")
@SpringBootTest(classes = ShellToolModesFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.shelltool")
public class ShellToolModesFeatureTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        ShellToolsAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "ShellToolModesFeatureTest");
    }

    @Test
    public void testDisabledModeSkipsToolRegistrationAndDeniesExecution() {
        liteflowConfig.getAgent().getShell().setMode(ShellMode.DISABLED);

        LiteflowResponse response = flowExecutor.execute2Resp(
                "shellToolsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Set<String> tools = ShellToolsAgentCmp.PROBE.get().toolNames();
        Assertions.assertFalse(tools.contains("execute_shell_command"),
                "Shell mode=DISABLED 时不应注册 execute_shell_command");

        // DISABLED 模式应返回拒绝消息。
        Assertions.assertNotNull(ShellToolsAgentCmp.PWD_OUTPUT.get());
        Assertions.assertTrue(ShellToolsAgentCmp.PWD_OUTPUT.get().contains("shell execution denied by policy"));
    }

    @Test
    public void testBlacklistModeRegistersToolAndBlocksDangerousFirstToken() {
        liteflowConfig.getAgent().getShell().setMode(ShellMode.BLACKLIST);

        LiteflowResponse response = flowExecutor.execute2Resp(
                "shellToolsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        Set<String> tools = ShellToolsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(tools.contains("execute_shell_command"),
                "Shell mode=BLACKLIST 时应注册 execute_shell_command");

        // pwd 在默认 blacklist 中不存在，应正常执行并返回当前 workspace。
        String pwd = ShellToolsAgentCmp.PWD_OUTPUT.get();
        Assertions.assertNotNull(pwd);
        Assertions.assertEquals(ShellToolsAgentCmp.WORKSPACE.get(), pwd.trim());

        // rm 在默认 blacklist 中，应返回 not allowed by blacklist。
        Assertions.assertTrue(ShellToolsAgentCmp.BLOCKED_OUTPUT.get().contains("not allowed by blacklist"));
    }

    @Test
    public void testWhitelistModeOnlyAllowsListedCommands() {
        liteflowConfig.getAgent().getShell().setMode(ShellMode.WHITELIST);
        liteflowConfig.getAgent().getShell().setWhitelist(List.of("pwd"));

        LiteflowResponse response = flowExecutor.execute2Resp(
                "shellToolsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        Set<String> tools = ShellToolsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(tools.contains("execute_shell_command"));

        // pwd 在白名单中，应执行成功。
        String pwd = ShellToolsAgentCmp.PWD_OUTPUT.get();
        Assertions.assertNotNull(pwd);
        Assertions.assertEquals(ShellToolsAgentCmp.WORKSPACE.get(), pwd.trim());

        // rm 不在白名单中，应被拒绝。
        Assertions.assertTrue(ShellToolsAgentCmp.BLOCKED_OUTPUT.get().contains("not allowed by whitelist"));
    }
}

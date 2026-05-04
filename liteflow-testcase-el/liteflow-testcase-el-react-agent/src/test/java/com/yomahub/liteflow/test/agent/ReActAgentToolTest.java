package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 测试 ReAct Agent 的工具注册行为。
 *
 * <p>工具分为三类：业务组件自定义工具、Workspace 文件工具和受控 Shell 工具。
 * 这个类分别验证默认工具集会注册，以及子类关闭内置工具时只保留自定义工具。
 */
public class ReActAgentToolTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证默认情况下会同时注册自定义工具、Workspace 文件工具和 Shell 工具。
     */
    @Test
    public void testStubAgentRegistersDefaultAndCustomTools() {
        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "tools");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();

        // 自定义工具来自测试组件覆写的 tools(ctx)。
        Assertions.assertTrue(toolNames.contains("custom_echo"));

        // Workspace 文件工具来自 ReActAgentComponent 的默认内置工具。
        Assertions.assertTrue(toolNames.contains("read_file"));
        Assertions.assertTrue(toolNames.contains("write_file"));
        Assertions.assertTrue(toolNames.contains("list_files"));
        Assertions.assertTrue(toolNames.contains("delete_file"));

        // Shell 工具受 liteflow.agent.shell.mode 控制，测试配置为 BLACKLIST，因此默认可注册。
        Assertions.assertTrue(toolNames.contains("execute_shell_command"));
    }

    /**
     * 验证子类可以关闭 ReActAgentComponent 默认注册的内置工具。
     */
    @Test
    public void testStubAgentCanDisableBuiltInTools() {
        StubReActAgentCmp.shellToolEnabled = false;
        StubReActAgentCmp.workspaceFileToolsEnabled = false;

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "disable-tools");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();

        // 关闭内置工具后，只应该留下组件自己声明的 custom_echo。
        Assertions.assertEquals(List.of("custom_echo"), toolNames);
    }

    /**
     * 验证子类自定义 handleReply 时，后置节点能读取到自定义后的响应数据。
     */
    @Test
    public void testStubAgentCanOverrideReplyHandling() {
        StubReActAgentCmp.customHandleReply = true;

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "custom-handler");

        Assertions.assertTrue(response.isSuccess());
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);

        // 测试桩会给默认回复加上 handled 前缀，证明 handleReply 覆写生效。
        Assertions.assertNotNull(reply);
        Assertions.assertTrue(reply.toString().startsWith("handled:reply:"));
    }
}

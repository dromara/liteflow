package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * 测试 ReAct Agent 的 Workspace 目录管理。
 *
 * <p>ReActAgentComponent 会通过 AgentSessionManager 为每个 session 创建独立工作目录。
 * 这个类只验证 workspace 的创建和 sessionId 到目录名的映射，不测试工具注册细节。
 */
public class ReActAgentWorkspaceTest extends AbstractReActAgentSpringbootTest {

    /**
     * 验证执行 agent 节点时会为当前 session 创建 workspace。
     */
    @Test
    public void testWorkspaceIsCreatedForResolvedSessionId() {
        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "workspace");

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertFalse(StubReActAgentCmp.MODEL_PROBES.isEmpty());

        StubReActAgentCmp.ModelProbe probe = StubReActAgentCmp.MODEL_PROBES.get(0);

        // 模型桩会记录 ReActAgentContext 中的 sessionId 和 workspace 路径。
        Assertions.assertEquals(StubReActAgentCmp.FIXED_SESSION_ID, probe.sessionId());
        Assertions.assertTrue(probe.workspaceExists());

        // 固定 sessionId 应该出现在 workspace 目录末尾，便于排查和隔离不同会话文件。
        Assertions.assertEquals(StubReActAgentCmp.FIXED_SESSION_ID,
                Path.of(probe.workspaceDir()).getFileName().toString());
    }
}

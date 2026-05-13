package com.yomahub.liteflow.test.agent.features.workspace;

import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
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
 * 覆盖 guide 中 workspace 文件工具的关键安全边界。
 *
 * <p>测试不让模型自行决定是否调用工具，而是在 Agent 生命周期内直接构造
 * {@link WorkspaceFileTools}，这样可以稳定验证工具行为，同时仍通过 THEN 链路触发。
 */
@TestPropertySource(value = "classpath:/agent/features/workspace/application.properties")
@SpringBootTest(classes = WorkspaceFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.workspace.cmp" })
public class WorkspaceFeatureTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ReActAgentFeatureTestSupport.ensureAgentConfig(
                liteflowConfig,
                "target/wk_react_agent_workspace",
                false,
                null,
                ShellMode.DISABLED);
        liteflowConfig.getAgent().getWorkspace().setMaxFileBytes(4);
        liteflowConfig.getAgent().getWorkspace().setMaxListSize(1);
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
        CompatibleCustomEchoAgentComponent.resetCompatibleProbe();
        WorkspaceFeatureProbe.reset();
    }

    @Test
    public void testWorkspaceToolsReadWriteListDeleteAndDenyEscapes() {
        LiteflowResponse response = flowExecutor.execute2Resp("workspaceFeatureChain", "workspace-tools");

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Assertions.assertEquals("abcd", WorkspaceFeatureProbe.TRUNCATED_READ.get(),
                "read_file 应按 max-file-bytes 截断读取");
        Assertions.assertEquals(1, WorkspaceFeatureProbe.LIST_RESULT.get().size(),
                "list_files 应按 max-list-size 限制返回数量");
        Assertions.assertTrue(WorkspaceFeatureProbe.RELATIVE_ESCAPE_DENIED.get().contains("path escapes workspace"));
        Assertions.assertTrue(WorkspaceFeatureProbe.ABSOLUTE_ESCAPE_DENIED.get().contains("absolute path denied"));
    }
}

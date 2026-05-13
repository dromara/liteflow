package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.WorkspaceToolsAgentCmp;
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
 * 覆盖 guide §6.3 workspace 文件工具：read/write/list/delete，
 * max-file-bytes 截断、max-list-size 限制、路径越界拒绝。
 *
 * <p>Agent 在 userPrompt 中直接调用 {@code WorkspaceFileTools} API，
 * 这样测试断言不依赖模型是否真的调用工具。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = WorkspaceToolsFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class WorkspaceToolsFeatureTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        WorkspaceToolsAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "WorkspaceToolsFeatureTest");
        // 配置极小阈值以验证截断行为
        liteflowConfig.getAgent().getWorkspace().setMaxFileBytes(4);
        liteflowConfig.getAgent().getWorkspace().setMaxListSize(1);
    }

    @Test
    public void testReadWriteListDeleteAndEscapeRejection() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "workspaceToolsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // read_file 在 max-file-bytes=4 时仅返回前 4 字节。
        Assertions.assertEquals("abcd", WorkspaceToolsAgentCmp.TRUNCATED_READ.get(),
                "read_file 应按 max-file-bytes 截断读取");

        // list_files 在 max-list-size=1 时只返回 1 条。
        Assertions.assertEquals(1, WorkspaceToolsAgentCmp.LIST_RESULT.get().size(),
                "list_files 应按 max-list-size 限制返回数量");

        // delete_file 成功删除。
        Assertions.assertEquals(Boolean.TRUE, WorkspaceToolsAgentCmp.DELETED.get(),
                "delete_file 应真的删除文件");

        // 越界路径被拒绝。
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.RELATIVE_ESCAPE.get());
        Assertions.assertTrue(WorkspaceToolsAgentCmp.RELATIVE_ESCAPE.get().contains("path escapes workspace"));
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.ABSOLUTE_ESCAPE.get());
        Assertions.assertTrue(WorkspaceToolsAgentCmp.ABSOLUTE_ESCAPE.get().contains("absolute path denied"));

        // 开启 workspace 文件工具时 4 个工具都应在 toolkit 中。
        Set<String> toolNames = WorkspaceToolsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(toolNames.contains("read_file"));
        Assertions.assertTrue(toolNames.contains("write_file"));
        Assertions.assertTrue(toolNames.contains("list_files"));
        Assertions.assertTrue(toolNames.contains("delete_file"));
        // Shell 关闭，不应注册。
        Assertions.assertFalse(toolNames.contains("execute_shell_command"));
    }
}

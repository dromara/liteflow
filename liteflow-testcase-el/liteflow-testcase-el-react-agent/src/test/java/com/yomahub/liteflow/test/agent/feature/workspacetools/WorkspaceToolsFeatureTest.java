package com.yomahub.liteflow.test.agent.feature.workspacetools;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

/**
 * 覆盖 guide §6.3 workspace 内置文件工具（AgentScope ReadFileTool / WriteFileTool）：
 * 读写回环、列目录、路径越界拒绝与 toolkit 注册形态。
 *
 * <p>Agent 在 userPrompt 中直接调用内置工具 API，断言不依赖模型是否真的调用工具。
 */
@TestPropertySource("classpath:/feature/workspacetools/application.properties")
@SpringBootTest(classes = WorkspaceToolsFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.workspacetools")
public class WorkspaceToolsFeatureTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        WorkspaceToolsAgentCmp.reset();
    }

    @Test
    public void testBuiltInFileToolsRoundtripAndEscapeRejection() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "workspaceToolsChain", "请用一句话作答。");

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // write_text_file 真的落盘，view_text_file 能读回内容。
        Assertions.assertEquals(Boolean.TRUE, WorkspaceToolsAgentCmp.FILE_EXISTS.get(),
                "write_text_file 应写入工作区文件");
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.READ_RESULT.get());
        Assertions.assertTrue(WorkspaceToolsAgentCmp.READ_RESULT.get().contains("abcdef"),
                "view_text_file 应读回写入的内容: " + WorkspaceToolsAgentCmp.READ_RESULT.get());

        // list_directory 能列出刚写入的文件。
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.LIST_RESULT.get());
        Assertions.assertTrue(WorkspaceToolsAgentCmp.LIST_RESULT.get().contains("a.txt"),
                "list_directory 应包含刚写入的文件: " + WorkspaceToolsAgentCmp.LIST_RESULT.get());

        // 越界路径被拒绝（相对路径穿越与绝对路径都不允许逃出 workspace root）。
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.RELATIVE_ESCAPE.get());
        Assertions.assertFalse(WorkspaceToolsAgentCmp.RELATIVE_ESCAPE.get().contains("abcdef"),
                "相对路径穿越不应读到工作区外的内容");
        Assertions.assertNotNull(WorkspaceToolsAgentCmp.ABSOLUTE_ESCAPE.get());
        Assertions.assertFalse(WorkspaceToolsAgentCmp.ABSOLUTE_ESCAPE.get().contains("abcdef"),
                "绝对路径不应逃出工作区");

        // 开启 workspace 文件工具时 4 个内置工具都应在 toolkit 中。
        Set<String> toolNames = WorkspaceToolsAgentCmp.PROBE.get().toolNames();
        Assertions.assertTrue(toolNames.contains("view_text_file"));
        Assertions.assertTrue(toolNames.contains("list_directory"));
        Assertions.assertTrue(toolNames.contains("write_text_file"));
        Assertions.assertTrue(toolNames.contains("insert_text_file"));
        // Shell 关闭，不应注册。
        Assertions.assertFalse(toolNames.contains("execute_shell_command"));
    }
}

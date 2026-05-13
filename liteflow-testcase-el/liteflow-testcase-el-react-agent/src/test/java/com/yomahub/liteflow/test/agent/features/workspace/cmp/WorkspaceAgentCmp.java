package com.yomahub.liteflow.test.agent.features.workspace.cmp;

import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import com.yomahub.liteflow.test.agent.features.workspace.WorkspaceFeatureProbe;
import org.springframework.stereotype.Component;

/**
 * 验证 WorkspaceFileTools 的具体行为，并将检查结果暴露给测试断言。
 */
@Component("workspaceAgent")
public class WorkspaceAgentCmp extends CompatibleCustomEchoAgentComponent {
    @Override
    protected String userPrompt() {
        WorkspaceFileTools tools = new WorkspaceFileTools(ctx().getWorkspaceDir(), agentConfig());
        tools.writeFile("notes/a.txt", "abcdef");
        tools.writeFile("notes/b.txt", "ghijkl");
        WorkspaceFeatureProbe.TRUNCATED_READ.set(tools.readFile("notes/a.txt"));
        WorkspaceFeatureProbe.LIST_RESULT.set(tools.listFiles("notes"));
        tools.deleteFile("notes/b.txt");
        try {
            tools.readFile("../escape.txt");
        } catch (SecurityException e) {
            WorkspaceFeatureProbe.RELATIVE_ESCAPE_DENIED.set(e.getMessage());
        }
        try {
            tools.readFile("/tmp/escape.txt");
        } catch (SecurityException e) {
            WorkspaceFeatureProbe.ABSOLUTE_ESCAPE_DENIED.set(e.getMessage());
        }
        return super.userPrompt();
    }
}

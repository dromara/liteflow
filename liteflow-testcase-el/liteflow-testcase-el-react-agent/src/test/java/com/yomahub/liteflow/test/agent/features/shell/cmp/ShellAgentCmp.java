package com.yomahub.liteflow.test.agent.features.shell.cmp;

import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.test.agent.features.shell.ShellFeatureProbe;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import org.springframework.stereotype.Component;

/**
 * 直接调用受管 Shell 工具，避免依赖模型是否主动选择工具。
 */
@Component("shellAgent")
public class ShellAgentCmp extends CompatibleCustomEchoAgentComponent {
    @Override
    protected String userPrompt() {
        ManagedShellCommandTool tool = new ManagedShellCommandTool(ctx().getWorkspaceDir(), agentConfig());
        ShellFeatureProbe.WORKSPACE.set(ctx().getWorkspaceDir().toAbsolutePath().normalize().toString());
        ShellFeatureProbe.PWD_OUTPUT.set(tool.executeCommand("pwd"));
        ShellFeatureProbe.DENIED_OUTPUT.set(tool.executeCommand("echo denied"));
        return super.userPrompt();
    }
}

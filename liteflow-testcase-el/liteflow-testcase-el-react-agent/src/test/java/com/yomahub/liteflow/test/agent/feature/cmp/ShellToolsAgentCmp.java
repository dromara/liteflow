package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 Shell 工具的 Agent，在 userPrompt 中直接调用 {@link ManagedShellCommandTool}
 * 验证 DISABLED / BLACKLIST / WHITELIST 三种模式下的工具行为。
 */
@Component("shellToolsAgent")
public class ShellToolsAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();
    public static final AtomicReference<String> PWD_OUTPUT = new AtomicReference<>();
    public static final AtomicReference<String> BLOCKED_OUTPUT = new AtomicReference<>();
    public static final AtomicReference<String> WORKSPACE = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
        PWD_OUTPUT.set(null);
        BLOCKED_OUTPUT.set(null);
        WORKSPACE.set(null);
    }

    @Override
    protected boolean enableShellTool() {
        return true;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }

    @Override
    protected String userPrompt() {
        ManagedShellCommandTool tool = new ManagedShellCommandTool(ctx().getWorkspaceDir(), agentConfig());
        WORKSPACE.set(ctx().getWorkspaceDir().toAbsolutePath().normalize().toString());
        PWD_OUTPUT.set(tool.executeCommand("pwd"));
        BLOCKED_OUTPUT.set(tool.executeCommand("rm -rf /"));
        return super.userPrompt();
    }
}

package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import com.yomahub.liteflow.test.agent.feature.probe.AgentProbe;
import io.agentscope.core.hook.Hook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 workspace 文件工具的 Agent，并在 userPrompt 中直接调用 {@link WorkspaceFileTools}
 * 验证其行为（read/write/list/delete/path-escape）。
 */
@Component("workspaceToolsAgent")
public class WorkspaceToolsAgentCmp extends AbstractCompatibleCustomAgentCmp {

    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static final AtomicReference<String> TRUNCATED_READ = new AtomicReference<>();
    public static final AtomicReference<List<String>> LIST_RESULT = new AtomicReference<>();
    public static final AtomicReference<String> RELATIVE_ESCAPE = new AtomicReference<>();
    public static final AtomicReference<String> ABSOLUTE_ESCAPE = new AtomicReference<>();
    public static final AtomicReference<Boolean> DELETED = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
        TRUNCATED_READ.set(null);
        LIST_RESULT.set(null);
        RELATIVE_ESCAPE.set(null);
        ABSOLUTE_ESCAPE.set(null);
        DELETED.set(null);
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return true;
    }

    @Override
    protected List<Hook> hooks() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.hook());
    }

    @Override
    protected String userPrompt() {
        WorkspaceFileTools tools = new WorkspaceFileTools(ctx().getWorkspaceDir(), agentConfig());
        tools.writeFile("notes/a.txt", "abcdef");
        tools.writeFile("notes/b.txt", "ghijkl");
        TRUNCATED_READ.set(tools.readFile("notes/a.txt"));
        LIST_RESULT.set(tools.listFiles("notes"));
        tools.deleteFile("notes/b.txt");
        DELETED.set(!java.nio.file.Files.exists(ctx().getWorkspaceDir().resolve("notes/b.txt")));
        try {
            tools.readFile("../escape.txt");
        } catch (SecurityException e) {
            RELATIVE_ESCAPE.set(e.getMessage());
        }
        try {
            tools.readFile("/tmp/escape.txt");
        } catch (SecurityException e) {
            ABSOLUTE_ESCAPE.set(e.getMessage());
        }
        return super.userPrompt();
    }
}

package com.yomahub.liteflow.test.agent.feature.workspacetools;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.tool.GuardedWorkspacePathResolver;
import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 workspace 文件工具的 Agent，并在 userPrompt 中直接调用 {@link WorkspaceFileTools}
 * 验证其行为（read/write/list/delete/path-escape），使断言不依赖模型是否真的调用工具。
 */
@Component("workspaceToolsAgent")
public class WorkspaceToolsAgentCmp extends ReActAgentComponent {

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
    protected ModelSpec<?> model() {
        throw new AssertionError("deterministic buildModel override must bypass model spec");
    }

    @Override
    protected Model buildModel() {
        return new DeterministicHistoryModel("workspace-tools-model", new java.util.ArrayList<>());
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow ReAct Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return true;
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.middleware());
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        java.nio.file.Path root = java.nio.file.Path.of(agentConfig().getWorkspace().getRoot());
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(
                root,
                agentConfig().getWorkspace().getMaxFileBytes(),
                agentConfig().getWorkspace().isAutoCreate());
        WorkspaceFileTools tools = new WorkspaceFileTools(resolver, agentConfig());
        io.agentscope.core.agent.RuntimeContext runtimeContext =
                io.agentscope.core.agent.RuntimeContext.builder()
                        .userId(context.getRuntimeUserId())
                        .sessionId(context.getRuntimeSessionId())
                        .put(com.yomahub.liteflow.agent.context.LiteFlowAgentContext.class, context)
                        .put(com.yomahub.liteflow.slot.Slot.class, context.getSlot())
                        .build();
        tools.writeFile(runtimeContext, "notes/a.txt", "abcd");
        tools.writeFile(runtimeContext, "notes/b.txt", "ghij");
        try {
            java.nio.file.Files.writeString(
                    resolver.resolve(context.getRuntimeSessionId(), "notes/a.txt"), "abcdef");
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to prepare oversized read fixture", failure);
        }
        TRUNCATED_READ.set(tools.readFile(runtimeContext, "notes/a.txt"));
        LIST_RESULT.set(tools.listFiles(runtimeContext, "notes"));
        tools.deleteFile(runtimeContext, "notes/b.txt");
        DELETED.set(!java.nio.file.Files.exists(
                resolver.resolve(context.getRuntimeSessionId(), "notes/b.txt")));
        try {
            tools.readFile(runtimeContext, "../escape.txt");
        } catch (SecurityException e) {
            RELATIVE_ESCAPE.set(e.getMessage());
        }
        try {
            tools.readFile(runtimeContext, "/tmp/escape.txt");
        } catch (SecurityException e) {
            ABSOLUTE_ESCAPE.set(e.getMessage());
        }
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}

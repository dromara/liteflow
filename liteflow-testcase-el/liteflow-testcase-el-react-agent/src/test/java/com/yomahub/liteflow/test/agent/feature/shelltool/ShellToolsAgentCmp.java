package com.yomahub.liteflow.test.agent.feature.shelltool;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.middleware.MiddlewareBase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 Shell 工具的 Agent，在 userPrompt 中直接调用 {@link ManagedShellCommandTool}
 * 验证 DISABLED / BLACKLIST / WHITELIST 三种模式下的工具行为。
 */
@Component("shellToolsAgent")
public class ShellToolsAgentCmp extends ReActAgentComponent {

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
    protected ModelSpec<?> model() {
        return LiveTestSupport.compatibleCustomModel();
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
        return true;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected List<MiddlewareBase> middlewares() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.middleware());
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        java.nio.file.Path root = java.nio.file.Path.of(agentConfig().getWorkspace().getRoot());
        ManagedShellCommandTool tool = new ManagedShellCommandTool(root, agentConfig());
        io.agentscope.core.agent.RuntimeContext runtimeContext =
                io.agentscope.core.agent.RuntimeContext.builder()
                        .userId(context.getRuntimeUserId())
                        .sessionId(context.getRuntimeSessionId())
                        .put(com.yomahub.liteflow.agent.context.LiteFlowAgentContext.class, context)
                        .put(com.yomahub.liteflow.slot.Slot.class, context.getSlot())
                        .build();
        WORKSPACE.set(root.resolve(context.getRuntimeSessionId()).toAbsolutePath().normalize().toString());
        PWD_OUTPUT.set(tool.executeCommand(runtimeContext, "pwd"));
        BLOCKED_OUTPUT.set(tool.executeCommand(runtimeContext, "rm -rf /"));
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }
}

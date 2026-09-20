package com.yomahub.liteflow.test.agent.feature.workspacetools;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the actual Harness file view without depending on model tool selection. */
@Component("workspaceToolsAgent")
public class WorkspaceToolsAgentCmp extends HarnessAgentComponent {
    private io.agentscope.harness.agent.filesystem.AbstractFilesystem filesystem;
    @Override protected com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime buildRuntime(
            com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext context) {
        var runtime = super.buildRuntime(context);
        filesystem = runtime.agent().getWorkspaceManager().getFilesystem();
        return runtime;
    }


    public static final AtomicReference<AgentProbe> PROBE = new AtomicReference<>();

    public static final AtomicReference<String> READ_RESULT = new AtomicReference<>();
    public static final AtomicReference<String> LIST_RESULT = new AtomicReference<>();
    public static final AtomicReference<String> RELATIVE_ESCAPE = new AtomicReference<>();
    public static final AtomicReference<String> ABSOLUTE_ESCAPE = new AtomicReference<>();
    public static final AtomicReference<Boolean> FILE_EXISTS = new AtomicReference<>();

    public static void reset() {
        PROBE.set(new AgentProbe());
        READ_RESULT.set(null);
        LIST_RESULT.set(null);
        RELATIVE_ESCAPE.set(null);
        ABSOLUTE_ESCAPE.set(null);
        FILE_EXISTS.set(null);
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
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
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
    protected List<MiddlewareBase> middlewares() {
        AgentProbe probe = PROBE.get();
        return probe == null ? List.of() : List.of(probe.middleware());
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        var rc = io.agentscope.core.agent.RuntimeContext.builder()
                .userId(null).sessionId(context.getRuntimeSessionId())
                .put(com.yomahub.liteflow.agent.context.LiteFlowAgentContext.class, context).build();
        filesystem.write(rc, "notes/a.txt", "abcdef");
        FILE_EXISTS.set(filesystem.exists(rc, "notes/a.txt"));
        READ_RESULT.set(filesystem.read(rc, "notes/a.txt", 0, 0).fileData().content());
        LIST_RESULT.set(filesystem.ls(rc, "notes").entries().toString());
        try { RELATIVE_ESCAPE.set(filesystem.read(rc, "../escape.txt", 0, 0).toString()); }
        catch (RuntimeException rejected) { RELATIVE_ESCAPE.set(rejected.getMessage()); }
        try { ABSOLUTE_ESCAPE.set(filesystem.read(rc, "/tmp/escape.txt", 0, 0).toString()); }
        catch (RuntimeException rejected) { ABSOLUTE_ESCAPE.set(rejected.getMessage()); }

        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

}

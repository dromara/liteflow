package com.yomahub.liteflow.test.agent.feature.workspacetools;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.DeterministicHistoryModel;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 开启 AgentScope 内置文件工具的 Agent，并在 userPrompt 中直接调用内置
 * {@code ReadFileTool}/{@code WriteFileTool} 验证读写、列目录与路径越界拒绝，
 * 使断言不依赖模型是否真的调用工具。
 */
@Component("workspaceToolsAgent")
public class WorkspaceToolsAgentCmp extends AgentComponent {

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
        String root = agentConfig().getWorkspace().getRoot();
        WriteFileTool writeFileTool = new WriteFileTool(root);
        ReadFileTool readFileTool = new ReadFileTool(root);

        writeFileTool.writeTextFile("notes/a.txt", "abcdef", null).block();
        FILE_EXISTS.set(java.nio.file.Files.exists(Path.of(root, "notes", "a.txt")));

        ToolResultBlock readResult = readFileTool.viewTextFile("notes/a.txt", null).block();
        READ_RESULT.set(render(readResult));

        ToolResultBlock listResult = readFileTool.listDirectory("notes").block();
        LIST_RESULT.set(render(listResult));

        ToolResultBlock relativeEscape = readFileTool.viewTextFile("../escape.txt", null).block();
        RELATIVE_ESCAPE.set(render(relativeEscape));

        ToolResultBlock absoluteEscape = readFileTool.viewTextFile("/tmp/escape.txt", null).block();
        ABSOLUTE_ESCAPE.set(render(absoluteEscape));

        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    private static String render(ToolResultBlock result) {
        if (result == null || result.getOutput() == null) {
            return null;
        }
        StringBuilder rendered = new StringBuilder();
        for (io.agentscope.core.message.ContentBlock block : result.getOutput()) {
            if (block instanceof io.agentscope.core.message.TextBlock textBlock) {
                rendered.append(textBlock.getText());
            } else {
                rendered.append(block);
            }
        }
        return rendered.toString();
    }
}

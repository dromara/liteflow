package com.yomahub.liteflow.test.agent.feature.multiagent;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * THEN 链路中先行 Agent：把 conversationId / agentKey / workspace 暴露出来，
 * 同时往 workspace 写入一个标记文件，供后续 Agent 读取以验证 workspace 共享。
 */
@Component("multiAgentA")
public class MultiAgentACmp extends ReActAgentComponent {

    public static final String MARKER_FILE = "from-a.txt";
    public static final String MARKER_CONTENT = "agent-a-was-here";

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<Path> SEEN_WORKSPACE = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
        SEEN_AGENT_KEY.set(null);
        SEEN_WORKSPACE.set(null);
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
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected String userPrompt() {
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        Path ws = ctx().getWorkspaceDir();
        SEEN_WORKSPACE.set(ws);
        try {
            Files.writeString(ws.resolve(MARKER_FILE), MARKER_CONTENT);
        } catch (IOException e) {
            throw new RuntimeException("write marker failed", e);
        }
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        if (reqData instanceof Map<?, ?> map) {
            Object p = map.get("prompt");
            if (p != null) {
                return p.toString();
            }
        }
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected void handleReply(Msg reply) {
        ctx().getSlot().setOutput("multiAgentA", reply == null ? null : reply.getTextContent());
    }
}

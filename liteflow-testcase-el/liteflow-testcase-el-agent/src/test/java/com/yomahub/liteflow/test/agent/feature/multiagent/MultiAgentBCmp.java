package com.yomahub.liteflow.test.agent.feature.multiagent;

import com.yomahub.liteflow.test.agent.support.OfflineAgentComponent;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * THEN 链路中后续 Agent：默认沿用前序 Agent 通过 slot.setConversationId() 写回的 cid，
 * 读取共享 workspace 中的标记文件以确认 workspace 同 conversation 共享。
 */
@Component("multiAgentB")
public class MultiAgentBCmp extends OfflineAgentComponent {

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<Path> SEEN_WORKSPACE = new AtomicReference<>();
    public static final AtomicReference<String> READ_MARKER = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
        SEEN_AGENT_KEY.set(null);
        SEEN_WORKSPACE.set(null);
        READ_MARKER.set(null);
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
        return false;
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        SEEN_CONVERSATION_ID.set(context.getConversationId());
        SEEN_AGENT_KEY.set(context.getAgentKey());
        Path ws = Path.of(agentConfig().getWorkspace().getRoot())
                .resolve(context.getRuntimeSessionId());
        SEEN_WORKSPACE.set(ws);
        try {
            Path marker = ws.resolve(MultiAgentACmp.MARKER_FILE);
            if (Files.exists(marker)) {
                READ_MARKER.set(Files.readString(marker));
            }
        } catch (IOException e) {
            throw new RuntimeException("read marker failed", e);
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
    protected void handleReply(Msg reply, com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        context.getSlot().setOutput("multiAgentB", reply == null ? null : reply.getTextContent());
    }
}

package com.yomahub.liteflow.test.agent.feature.cmp;

import com.yomahub.liteflow.test.agent.cmp.AbstractCompatibleCustomAgentCmp;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * THEN 链路中后续 Agent：默认沿用前序 Agent 通过 slot.setConversationId() 写回的 cid，
 * 读取共享 workspace 中的标记文件以确认 workspace 同 conversation 共享。
 */
@Component("multiAgentB")
public class MultiAgentBCmp extends AbstractCompatibleCustomAgentCmp {

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
    protected String userPrompt() {
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        Path ws = ctx().getWorkspaceDir();
        SEEN_WORKSPACE.set(ws);
        try {
            Path marker = ws.resolve(MultiAgentACmp.MARKER_FILE);
            if (Files.exists(marker)) {
                READ_MARKER.set(Files.readString(marker));
            }
        } catch (IOException e) {
            throw new RuntimeException("read marker failed", e);
        }
        return super.userPrompt();
    }

    @Override
    protected void handleReply(Msg reply) {
        ctx().getSlot().setOutput("multiAgentB", reply == null ? null : reply.getTextContent());
    }
}

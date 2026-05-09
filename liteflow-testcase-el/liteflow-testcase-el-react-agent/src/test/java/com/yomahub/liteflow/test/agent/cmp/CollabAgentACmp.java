package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 协作场景中的"先行 agent"：固定一个 conversationId 并把 workspace 路径暴露出来，
 * 同时往 workspace 写入一个标记文件，验证后续 agent 能在同一目录中读到。
 */
@Component("collabAgentA")
public class CollabAgentACmp extends ReActAgentComponent {

    public static final String MARKER_FILE = "from-a.txt";
    public static final String MARKER_CONTENT = "hello-from-a";

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_WORKSPACE = new AtomicReference<>();
    /**
     * 进入 {@link #userPrompt()} 时 marker 文件是否已存在。
     * 在跨次调用复用同一 conversationId 的场景下，第二次调用看到的应该是 {@code true}，
     * 证明 workspace 状态被保留。
     */
    public static final AtomicReference<Boolean> MARKER_EXISTED_BEFORE_WRITE = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
        SEEN_AGENT_KEY.set(null);
        SEEN_WORKSPACE.set(null);
        MARKER_EXISTED_BEFORE_WRITE.set(null);
    }

    @Override
    protected ModelSpec<?> model() {
        return new SilentModelSpec();
    }

    @Override
    protected String systemPrompt() {
        return "system-a";
    }

    @Override
    protected String userPrompt() {
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        SEEN_WORKSPACE.set(ctx().getWorkspaceDir().toString());
        try {
            Path marker = ctx().getWorkspaceDir().resolve(MARKER_FILE);
            MARKER_EXISTED_BEFORE_WRITE.set(Files.exists(marker));
            Files.writeString(marker, MARKER_CONTENT);
        } catch (IOException e) {
            throw new RuntimeException("collab agent A failed to write marker", e);
        }
        return "A says hi";
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

    static class SilentModelSpec extends ModelSpec<SilentModelSpec> {
        @Override
        public Model resolve(AgentConfig cfg) {
            return new SilentModel();
        }
    }

    static class SilentModel implements Model {
        @Override
        public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text("ok-a").build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "silent-model-a";
        }
    }
}

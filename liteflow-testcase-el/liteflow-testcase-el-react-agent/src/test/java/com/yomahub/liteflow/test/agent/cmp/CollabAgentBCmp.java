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
 * 协作场景中的"后续 agent"：不覆写 {@code resolveConversationId}，因此会自动复用
 * 前序 agent 通过 {@code slot.setConversationId(...)} 写回的会话标识；
 * 通过读取 workspace 中的标记文件来证明 workspace 在同 chain 内确实是共享的。
 */
@Component("collabAgentB")
public class CollabAgentBCmp extends ReActAgentComponent {

    public static final AtomicReference<String> SEEN_CONVERSATION_ID = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_AGENT_KEY = new AtomicReference<>();
    public static final AtomicReference<String> SEEN_WORKSPACE = new AtomicReference<>();
    public static final AtomicReference<String> READ_MARKER = new AtomicReference<>();

    public static void reset() {
        SEEN_CONVERSATION_ID.set(null);
        SEEN_AGENT_KEY.set(null);
        SEEN_WORKSPACE.set(null);
        READ_MARKER.set(null);
    }

    @Override
    protected ModelSpec<?> model() {
        return new SilentModelSpec();
    }

    @Override
    protected String systemPrompt() {
        return "system-b";
    }

    @Override
    protected String userPrompt() {
        SEEN_CONVERSATION_ID.set(ctx().getConversationId());
        SEEN_AGENT_KEY.set(ctx().getAgentKey());
        SEEN_WORKSPACE.set(ctx().getWorkspaceDir().toString());
        try {
            Path marker = ctx().getWorkspaceDir().resolve(CollabAgentACmp.MARKER_FILE);
            if (Files.exists(marker)) {
                READ_MARKER.set(Files.readString(marker));
            }
        } catch (IOException e) {
            throw new RuntimeException("collab agent B failed to read marker", e);
        }
        return "B reads marker";
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
                    .content(List.of(TextBlock.builder().text("ok-b").build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "silent-model-b";
        }
    }
}

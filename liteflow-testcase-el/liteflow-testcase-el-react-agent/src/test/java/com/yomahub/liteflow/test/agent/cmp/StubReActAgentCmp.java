package com.yomahub.liteflow.test.agent.cmp;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component("stubAgent")
public class StubReActAgentCmp extends ReActAgentComponent {

    public static final String FIXED_CONVERSATION_ID = "fixed-conversation";
    public static final AtomicInteger SPEC_RESOLVE_COUNT = new AtomicInteger();
    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();
    public static final AtomicInteger SYSTEM_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger USER_PROMPT_COUNT = new AtomicInteger();
    public static final AtomicInteger HANDLE_REPLY_COUNT = new AtomicInteger();
    public static final AtomicInteger CUSTOM_TOOL_REGISTER_COUNT = new AtomicInteger();
    public static final AtomicInteger HOOK_EVENT_COUNT = new AtomicInteger();
    public static final List<Integer> MAX_ITERATIONS_SEEN = new CopyOnWriteArrayList<>();
    public static final List<String> USER_PROMPTS = new CopyOnWriteArrayList<>();
    public static final List<ModelProbe> MODEL_PROBES = new CopyOnWriteArrayList<>();
    public static volatile List<String> allowedSkills = List.of();
    public static final List<List<String>> USED_SKILL_PROBES = new CopyOnWriteArrayList<>();
    public static volatile boolean shellToolEnabled = true;
    public static volatile boolean workspaceFileToolsEnabled = true;
    public static volatile boolean customHandleReply = false;
    public static volatile int overriddenMaxIterations = -1;

    public static void reset() {
        SPEC_RESOLVE_COUNT.set(0);
        BUILD_MODEL_COUNT.set(0);
        SYSTEM_PROMPT_COUNT.set(0);
        USER_PROMPT_COUNT.set(0);
        HANDLE_REPLY_COUNT.set(0);
        CUSTOM_TOOL_REGISTER_COUNT.set(0);
        HOOK_EVENT_COUNT.set(0);
        MAX_ITERATIONS_SEEN.clear();
        USER_PROMPTS.clear();
        MODEL_PROBES.clear();
        allowedSkills = List.of();
        USED_SKILL_PROBES.clear();
        shellToolEnabled = true;
        workspaceFileToolsEnabled = true;
        customHandleReply = false;
        overriddenMaxIterations = -1;
    }

    @Override
    protected ModelSpec<?> model() {
        return new StubModelSpec(this);
    }

    @Override
    protected String systemPrompt() {
        SYSTEM_PROMPT_COUNT.incrementAndGet();
        return "system:" + ctx().getConversationId() + ":" + ctx().getAgentKey();
    }

    @Override
    protected String userPrompt() {
        USER_PROMPT_COUNT.incrementAndGet();
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        String prompt = reqData == null ? "" : reqData.toString();
        USER_PROMPTS.add(prompt);
        return prompt;
    }

    @Override
    protected List<Object> tools() {
        CUSTOM_TOOL_REGISTER_COUNT.incrementAndGet();
        return List.of(new EchoTool());
    }

    @Override
    protected List<String> skills() {
        return allowedSkills;
    }

    @Override
    protected String resolveConversationId() {
        return FIXED_CONVERSATION_ID;
    }

    @Override
    protected int maxIterations() {
        return overriddenMaxIterations;
    }

    @Override
    protected boolean enableShellTool() {
        return shellToolEnabled;
    }

    @Override
    protected boolean enableWorkspaceFileTools() {
        return workspaceFileToolsEnabled;
    }

    @Override
    protected List<Hook> hooks() {
        return List.of(new Hook() {
            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                HOOK_EVENT_COUNT.incrementAndGet();
                if (event.getAgent() instanceof ReActAgent agent) {
                    MAX_ITERATIONS_SEEN.add(agent.getMaxIters());
                }
                return Mono.just(event);
            }
        });
    }

    @Override
    protected boolean enableReActLogging() {
        return false;
    }

    @Override
    protected void handleReply(Msg reply) {
        USED_SKILL_PROBES.add(usedSkills());
        HANDLE_REPLY_COUNT.incrementAndGet();
        if (customHandleReply) {
            ctx().getSlot().setResponseData("handled:" + (reply == null ? null : reply.getTextContent()));
            return;
        }
        super.handleReply(reply);
    }

    public static class StubModelSpec extends ModelSpec<StubModelSpec> {
        private final StubReActAgentCmp comp;

        StubModelSpec(StubReActAgentCmp comp) {
            this.comp = comp;
        }

        @Override
        public Model resolve(AgentConfig cfg) {
            SPEC_RESOLVE_COUNT.incrementAndGet();
            BUILD_MODEL_COUNT.incrementAndGet();
            return new StubModel(comp);
        }
    }

    public static class StubModel implements Model {
        private final StubReActAgentCmp comp;
        private final AtomicInteger callCount = new AtomicInteger();
        private volatile String lastConversationId;
        private volatile String lastAgentKey;
        private volatile String lastWorkspaceDir;
        private volatile boolean lastWorkspaceExists;

        StubModel(StubReActAgentCmp comp) {
            this.comp = comp;
        }

        @Override
        public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas, GenerateOptions options) {
            try {
                var ctx = comp.ctx();
                lastConversationId = ctx.getConversationId();
                lastAgentKey = ctx.getAgentKey();
                lastWorkspaceDir = ctx.getWorkspaceDir().toString();
                lastWorkspaceExists = Files.isDirectory(ctx.getWorkspaceDir());
            } catch (IllegalStateException | NullPointerException ignored) {
                // Agentscope may call the model again from a worker thread after tool execution.
                // The test model reuses the invocation metadata captured from the first call.
            }
            List<String> toolNames = toolSchemas == null ? List.of() : toolSchemas.stream()
                    .map(ToolSchema::getName)
                    .sorted()
                    .toList();
            List<String> inputTexts = messages == null ? List.of() : messages.stream().map(Msg::getTextContent).toList();
            int currentCall = callCount.incrementAndGet();
            ModelProbe probe = new ModelProbe(
                    lastConversationId,
                    lastAgentKey,
                    lastWorkspaceDir,
                    lastWorkspaceExists,
                    currentCall,
                    inputTexts,
                    toolNames,
                    options == null ? null : options.getTemperature());
            MODEL_PROBES.add(probe);

            if (currentCall == 1 && inputTexts.contains("load-demo-skill")) {
                return Flux.just(ChatResponse.builder()
                        .content(List.of(new ToolUseBlock(
                                "load-demo-skill-tool-call",
                                "load_skill_through_path",
                                Map.of("skillId", "demo_filesystem-agent_skills", "path", "SKILL.md"),
                                "{\"skillId\":\"demo_filesystem-agent_skills\",\"path\":\"SKILL.md\"}",
                                null)))
                        .finishReason("tool_calls")
                        .build());
            }

            String text = "reply:" + probe.conversationId + ":" + probe.callCount + ":" + probe.inputTexts;
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text(text).build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "stub-model";
        }
    }

    public static class EchoTool {
        @Tool(name = "custom_echo", description = "Return the provided value")
        public String echo(@ToolParam(name = "value", description = "value") String value) {
            return value;
        }
    }

    public record ModelProbe(
            String conversationId,
            String agentKey,
            String workspaceDir,
            boolean workspaceExists,
            int callCount,
            List<String> inputTexts,
            List<String> toolNames,
            Double temperature) {
    }
}

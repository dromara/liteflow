package com.yomahub.liteflow.test.agent.feature.sandbox;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component("guardedLocalAgent")
public class GuardedLocalSandboxAgentCmp extends HarnessAgentComponent {
    @Override protected boolean enableShellTool() { return false; }


    private static final AtomicInteger DOCKER_CLIENT_CALLS = new AtomicInteger();

    static void reset() {
        DOCKER_CLIENT_CALLS.set(0);
    }

    static int dockerClientCalls() {
        return DOCKER_CLIENT_CALLS.get();
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        return new GuardedWriteModel();
    }

    @Override
    protected SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
        DOCKER_CLIENT_CALLS.incrementAndGet();
        return null;
    }

    @Override
    protected PermissionContextState permissionContext() {
        return PermissionContextState.builder()
                .addAllowRule("write_file", new PermissionRule(
                        "write_file", null, PermissionBehavior.ALLOW,
                        "guarded local filesystem test"))
                .build();
    }

    @Override
    protected String systemPrompt() {
        return "Exercise only the guarded local filesystem.";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return "write deterministic files";
    }

    private static final class GuardedWriteModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            if (calls.getAndIncrement() == 0) {
                if (tools.stream().noneMatch(tool -> "write_file".equals(tool.getName()))) {
                    return Flux.error(new AssertionError("write_file tool is missing"));
                }
                ToolUseBlock inside = write(
                        "write-inside", "notes/result.txt", "inside-workspace");
                ToolUseBlock escape = write(
                        "write-escape", "../guarded-local-escape-probe.txt", "must-not-exist");
                return Flux.just(ChatResponse.builder()
                        .content(List.of(inside, escape))
                        .finishReason("tool_calls")
                        .build());
            }
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text("guarded-local-ok").build()))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "offline-guarded-local-model";
        }

        private static ToolUseBlock write(String id, String path, String content) {
            Map<String, Object> input = Map.of("path", path, "content", content);
            return new ToolUseBlock(
                    id,
                    "write_file",
                    input,
                    "{\"path\":\"" + path + "\",\"content\":\"" + content + "\"}",
                    Map.of(),
                    ToolCallState.PENDING);
        }
    }
}

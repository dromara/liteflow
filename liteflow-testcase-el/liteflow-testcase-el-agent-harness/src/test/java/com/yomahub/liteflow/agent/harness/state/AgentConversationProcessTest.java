package com.yomahub.liteflow.agent.harness.state;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Actual process boundaries, with deterministic model output rather than a paid model dependency. */
class AgentConversationProcessTest {
    @TempDir Path temp;

    @Test void jsonConversationAndDeletionSurviveActualJvmRestarts() throws Exception {
        roundTrip(AgentSessionStoreType.JSON);
    }

    private void roundTrip(AgentSessionStoreType type) throws Exception {
        String conversation = UUID.randomUUID().toString();
        run(type, conversation, "write");
        run(type, conversation, "read");
        try (var service = AgentConversationService.open(configuration(type, temp))) {
            assertEquals(4, service.messages(conversation, 0, 10).items().size());
            assertTrue(service.agentState(conversation, "restart-agent").isPresent());
            service.delete(conversation);
            assertTrue(service.agentState(conversation, "restart-agent").isEmpty());
        }
        run(type, conversation, "deleted");
    }

    private void run(AgentSessionStoreType type, String conversation, String mode) throws Exception {
        Process process = start(type, conversation, mode);
        try { finish(process, mode); }
        finally { if (process.isAlive()) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS); }
    }

    private Process start(AgentSessionStoreType type, String conversation, String mode) throws Exception {
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                Probe.class.getName(), type.name(), temp.toString(), conversation, mode)
                .redirectErrorStream(true).redirectOutput(temp.resolve(mode + ".log").toFile()).start();
    }

    private void finish(Process process, String mode) throws Exception {
        assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Child JVM timed out: " + mode);
        String log = Files.readString(temp.resolve(mode + ".log"));
        assertEquals(0, process.exitValue(), log);
        assertTrue(log.contains("PROBE-OK:" + mode), log);
    }

    private static AgentConfig configuration(AgentSessionStoreType type, Path root) {
        AgentConfig config = new AgentConfig();
        config.setApplicationName("process-guide");
        config.setExecutionTimeout(Duration.ofSeconds(8));
        config.getInvocationGuard().setLeaseDuration(Duration.ofMillis(600));
        config.getSessionStore().setType(type);
        config.getSessionStore().setJsonRoot(root.resolve("state").toString());
        config.getSessionStore().setJsonWorkspaceRoot(root.resolve("workspace").toString());
        return config;
    }

    public static final class Probe {
        public static void main(String[] args) throws Exception {
            AgentSessionStoreType type = AgentSessionStoreType.valueOf(args[0]);
            Path root = Path.of(args[1]);
            String conversation = args[2];
            String mode = args[3];
            AgentConfig config = configuration(type, root);
            LiteflowConfig liteflow = new LiteflowConfig();
            liteflow.setAgent(config);
            LiteflowConfigGetter.setLiteflowConfig(liteflow);
            try {
                try (var component = new Component(conversation, mode)) {
                    if (mode.equals("deleted")) {
                        assertThrows(RuntimeException.class, component::process);
                        assertEquals(0, component.calls);
                    } else {
                        component.process();
                        assertEquals(1, component.calls);
                    }
                }
                System.out.println("PROBE-OK:" + mode);
            } finally { LiteflowConfigGetter.clean(); }
        }
    }

    private static final class Component extends HarnessAgentComponent {
        final Slot slot = new Slot();
        final String mode;
        int calls;
        Component(String conversation, String mode) {
            this.mode = mode;
            setNodeId("restart-agent");
            slot.setConversationId(conversation);
            slot.setChainId("restart");
        }
        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("deterministic model"); }
        @Override protected boolean enableShellTool() { return false; }
        @Override protected String systemPrompt() { return "Answer using your previous context."; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return mode; }
        @Override protected Model buildModel() {
            return new Model() {
                @Override public String getModelName() { return "process-probe"; }
                @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                    calls++;
                    boolean restored = messages.stream().anyMatch(message -> message.getTextContent().contains("retained-marker"));
                    assertEquals(mode.equals("read"), restored);
                    return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder()
                            .text("retained-marker").build())).finishReason("stop").build());
                }
            };
        }
    }
}

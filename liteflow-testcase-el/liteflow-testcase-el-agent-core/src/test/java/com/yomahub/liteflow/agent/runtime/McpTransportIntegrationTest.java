package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Guide 4.2: actual discovery and calls over each supported transport, without an LLM account. */
class McpTransportIntegrationTest {
    @TempDir Path temp;
    @AfterEach void cleanup() { LiteflowConfigGetter.clean(); }

    @ParameterizedTest
    @ValueSource(strings = {"http", "sse", "stdio"})
    void discoveredRemoteToolIsCalledByHarnessAndItsResultReachesTheModel(String transport) throws Exception {
        configure();
        try (var server = new McpTestServer();
             var client = client(transport, server, "Bearer guide-test").buildSync()) {
            AtomicInteger modelCalls = new AtomicInteger();
            try (var component = new McpComponent(client, modelCalls, "123", "ORDER:123:PAID")) {
                component.process();
                assertEquals("verified remote order", component.slot.getResponseData());
                assertEquals(2, modelCalls.get());
            }
            // Borrowed clients remain usable after the component runtime closes.
            assertEquals("lookup_order", client.listTools().block(Duration.ofSeconds(3)).get(0).name());
            if (!transport.equals("stdio")) {
                assertEquals(1, server.calls.get());
                assertTrue(server.authorizedRequests.get() >= 3);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "sse"})
    void serverRejectsMissingOrIncorrectAuthorizationBeforeAnyToolCall(String transport) throws Exception {
        try (var server = new McpTestServer()) {
            assertThrows(RuntimeException.class, () -> {
                try (var client = client(transport, server, "Bearer invalid").buildSync()) {
                    client.initialize().block(Duration.ofSeconds(3));
                }
            });
            assertEquals(0, server.calls.get());
            assertEquals(0, server.authorizedRequests.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "sse", "stdio"})
    void remoteToolErrorIsVisibleToTheAgentForRecovery(String transport) throws Exception {
        configure();
        try (var server = new McpTestServer();
             var client = client(transport, server, "Bearer guide-test").buildSync();
             var component = new McpComponent(client, new AtomicInteger(), "missing", "ORDER-NOT-FOUND")) {
            component.process();
            assertEquals("verified remote order", component.slot.getResponseData());
        }
    }

    private McpClientBuilder client(String transport, McpTestServer server, String authorization) {
        var builder = McpClientBuilder.create("orders").timeout(Duration.ofSeconds(2))
                .initializationTimeout(Duration.ofSeconds(2));
        return switch (transport) {
            case "http" -> builder.streamableHttpTransport(server.url("/mcp")).header("Authorization", authorization);
            case "sse" -> builder.sseTransport(server.url("/sse")).header("Authorization", authorization);
            case "stdio" -> builder.stdioTransport(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                    McpTestServer.class.getName());
            default -> throw new AssertionError(transport);
        };
    }

    private void configure() {
        var agent = new AgentConfig();
        agent.setApplicationName("mcp-guide");
        agent.setExecutionTimeout(Duration.ofSeconds(10));
        agent.getSessionStore().setJsonRoot(temp.resolve("state").toString());
        agent.getSessionStore().setJsonWorkspaceRoot(temp.resolve("workspace").toString());
        var config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private static final class McpComponent extends HarnessAgentComponent {
        final Slot slot = new Slot();
        private final McpClientWrapper client;
        private final Model model;

        McpComponent(McpClientWrapper client, AtomicInteger calls, String orderId, String expected) {
            this.client = client;
            setNodeId("orders-agent");
            slot.setChainId("mcp-guide");
            slot.setConversationId("conversation");
            model = new Model() {
                @Override public String getModelName() { return "mcp-scripted"; }
                @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                    if (calls.incrementAndGet() == 1) {
                        ToolSchema remote = tools.stream().filter(t -> t.getName().equals("lookup_order")).findFirst().orElseThrow();
                        assertTrue(remote.getParameters().toString().contains("orderId"));
                        return Flux.just(ChatResponse.builder().content(List.of(new ToolUseBlock(
                                "lookup-1", "lookup_order", Map.of("orderId", orderId),
                                "{\"orderId\":\"" + orderId + "\"}", Map.of(), ToolCallState.PENDING)))
                                .finishReason("tool_calls").build());
                    }
                    assertTrue(messages.stream().flatMap(m -> m.getContent().stream())
                            .filter(ToolResultBlock.class::isInstance).map(ToolResultBlock.class::cast)
                            .flatMap(result -> result.getOutput().stream()).filter(TextBlock.class::isInstance)
                            .map(TextBlock.class::cast).anyMatch(text -> text.getText().contains(expected)),
                            () -> "remote output must be passed back to the model: " + messages);
                    return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder()
                            .text("verified remote order").build())).finishReason("stop").build());
                }
            };
        }
        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("use scripted model"); }
        @Override protected Model buildModel() { return model; }
        @Override protected boolean enableShellTool() { return false; }
        @Override protected String systemPrompt() { return "Use the order tool"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "Check order"; }
        @Override protected List<McpClientWrapper> mcpClients() { return List.of(client); }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder.disableMemoryHooks().disableWorkspaceContext().disableDefaultWorkspaceSkills();
        }
    }
}

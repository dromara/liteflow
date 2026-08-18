package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientLifecycleTest {

    private static final String AGENT_NAMESPACE =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    void registrationInitializesAndListsEachIdentityOnceAndRuntimeClosesOnlyOwnedClientsOnce() {
        FakeMcpClient owned = FakeMcpClient.success("owned");
        FakeMcpClient borrowed = FakeMcpClient.success("borrowed");
        TestComponent component = new TestComponent(
                List.of(owned, borrowed, owned), List.of(owned));

        ReActAgentRuntime runtime = component.runtime(config(Duration.ofSeconds(1)));
        runtime.close();
        runtime.close();

        assertEquals(1, owned.initializeCount.get());
        assertEquals(1, owned.listCount.get());
        assertEquals(1, owned.closeCount.get());
        assertEquals(1, borrowed.initializeCount.get());
        assertEquals(1, borrowed.listCount.get());
        assertEquals(0, borrowed.closeCount.get());
    }

    @Test
    void nthRegistrationFailureClosesPriorAndFailingOwnedClientsButNeverBorrowedClients() {
        FakeMcpClient priorOwned = FakeMcpClient.success("prior-owned");
        RuntimeException registrationFailure = new RuntimeException("list failed");
        FakeMcpClient failingBorrowed = FakeMcpClient.listFailure(
                "failing-borrowed", registrationFailure);
        FakeMcpClient neverReachedOwned = FakeMcpClient.success("never-reached");
        TestComponent component = new TestComponent(
                List.of(priorOwned, failingBorrowed, neverReachedOwned),
                List.of(priorOwned, neverReachedOwned));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> component.runtime(config(Duration.ofSeconds(1))));

        assertSame(registrationFailure, rootCause(thrown));
        assertEquals(1, priorOwned.closeCount.get());
        assertEquals(0, failingBorrowed.closeCount.get());
        assertEquals(0, neverReachedOwned.initializeCount.get());
        assertEquals(0, neverReachedOwned.closeCount.get());
    }

    @Test
    void priorBorrowedClientStaysOpenWhenNthOwnedClientFailsRegistration() {
        FakeMcpClient priorBorrowed = FakeMcpClient.success("prior-borrowed");
        RuntimeException registrationFailure = new RuntimeException("owned list failed");
        FakeMcpClient failingOwned = FakeMcpClient.listFailure(
                "failing-owned", registrationFailure);
        TestComponent component = new TestComponent(
                List.of(priorBorrowed, failingOwned), List.of(failingOwned));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> component.runtime(config(Duration.ofSeconds(1))));

        assertSame(registrationFailure, rootCause(thrown));
        assertEquals(1, priorBorrowed.initializeCount.get());
        assertEquals(0, priorBorrowed.closeCount.get());
        assertEquals(1, failingOwned.initializeCount.get());
        assertEquals(1, failingOwned.closeCount.get());
    }

    @Test
    void failedOrTimedOutOwnedClientIsClosedAndNoRuntimeIsPublished() {
        FakeMcpClient failingOwned = FakeMcpClient.listFailure(
                "failing-owned", new IllegalStateException("boom"));
        TestComponent failing = new TestComponent(List.of(failingOwned), List.of(failingOwned));

        assertThrows(RuntimeException.class,
                () -> failing.runtime(config(Duration.ofSeconds(1))));
        assertEquals(1, failingOwned.closeCount.get());

        FakeMcpClient timedOutOwned = FakeMcpClient.neverInitializes("timed-out-owned");
        TestComponent timedOut = new TestComponent(
                List.of(timedOutOwned), List.of(timedOutOwned));
        assertThrows(RuntimeException.class,
                () -> timedOut.runtime(config(Duration.ofMillis(50))));
        assertEquals(1, timedOutOwned.closeCount.get());
    }

    @Test
    void closeFailuresAreSuppressedWithoutMaskingTheBuildFailure() {
        RuntimeException registrationFailure = new RuntimeException("registration failed");
        FakeMcpClient first = FakeMcpClient.success("first");
        first.closeFailure = new IllegalStateException("close first");
        FakeMcpClient second = FakeMcpClient.listFailure("second", registrationFailure);
        second.closeFailure = new IllegalArgumentException("close second");
        TestComponent component = new TestComponent(
                List.of(first, second), List.of(first, second));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> component.runtime(config(Duration.ofSeconds(1))));

        assertSame(registrationFailure, rootCause(thrown));
        assertTrue(thrown.getSuppressed().length > 0
                        || registrationFailure.getSuppressed().length > 0,
                "owned close failures must remain attached as suppressed failures");
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static AgentConfig config(Duration timeout) {
        AgentConfig config = new AgentConfig();
        config.getStateStore().setJsonRoot("target/agent-state");
        config.getRuntime().setNamespace("mcp-test");
        config.getRuntime().setTimeout(timeout);
        return config;
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Model model = new ScriptedChatModel("reply");
        private final List<McpClientWrapper> clients;
        private final List<McpClientWrapper> owned;

        private TestComponent(List<McpClientWrapper> clients, List<McpClientWrapper> owned) {
            this.clients = clients;
            this.owned = owned;
        }

        private ReActAgentRuntime runtime(AgentConfig config) {
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "mcp-agent", "agent-key", AGENT_NAMESPACE));
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            return model;
        }

        @Override
        protected String systemPrompt() {
            return "mcp test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }

        @Override
        protected List<McpClientWrapper> mcpClients() {
            return clients;
        }

        @Override
        protected boolean ownsMcpClient(McpClientWrapper client) {
            return owned.stream().anyMatch(candidate -> candidate == client);
        }
    }

    private static final class FakeMcpClient extends McpClientWrapper {
        private final Mono<Void> initialization;
        private final Mono<List<McpSchema.Tool>> tools;
        private final AtomicInteger initializeCount = new AtomicInteger();
        private final AtomicInteger listCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();
        private RuntimeException closeFailure;

        private FakeMcpClient(
                String name, Mono<Void> initialization, Mono<List<McpSchema.Tool>> tools) {
            super(name);
            this.initialization = initialization;
            this.tools = tools;
        }

        private static FakeMcpClient success(String name) {
            return new FakeMcpClient(name, Mono.empty(), Mono.just(List.of()));
        }

        private static FakeMcpClient listFailure(String name, RuntimeException failure) {
            return new FakeMcpClient(name, Mono.empty(), Mono.error(failure));
        }

        private static FakeMcpClient neverInitializes(String name) {
            return new FakeMcpClient(name, Mono.never(), Mono.just(List.of()));
        }

        @Override
        public Mono<Void> initialize() {
            initializeCount.incrementAndGet();
            return initialization;
        }

        @Override
        public Mono<List<McpSchema.Tool>> listTools() {
            listCount.incrementAndGet();
            return tools;
        }

        @Override
        public Mono<McpSchema.CallToolResult> callTool(
                String toolName, Map<String, Object> arguments) {
            return Mono.error(new UnsupportedOperationException("not used"));
        }

        @Override
        public Mono<McpSchema.CallToolResult> callTool(
                String toolName, Map<String, Object> arguments, Map<String, Object> meta) {
            return Mono.error(new UnsupportedOperationException("not used"));
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }
}

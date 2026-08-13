package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardedWorkspacePathResolverTest {

    @TempDir
    Path root;

    @Test
    void rejectsUnixWindowsUncNulAndEveryParentTraversalComponent() {
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 32);

        for (String denied : List.of(
                "/etc/passwd",
                "C:\\Windows\\system.ini",
                "C:/Windows/system.ini",
                "\\\\server\\share\\file",
                "//server/share/file",
                "../outside",
                "safe/../outside",
                "safe\\..\\outside",
                "bad\0path")) {
            assertThrows(SecurityException.class,
                    () -> resolver.resolve("runtime-session", denied), denied);
        }
    }

    @Test
    void hashesSessionIdsAndKeepsExistingAndNewTargetsInsideTheRealSessionRoot()
            throws IOException {
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 32);
        Path sessionRoot = resolver.sessionRoot("tenant/conversation with spaces");
        Path existing = sessionRoot.resolve("existing.txt");
        Files.writeString(existing, "value", StandardCharsets.UTF_8);

        assertFalse(sessionRoot.toString().contains("tenant"));
        assertTrue(resolver.resolve("tenant/conversation with spaces", "existing.txt")
                .toRealPath().startsWith(sessionRoot.toRealPath()));
        assertTrue(resolver.resolve("tenant/conversation with spaces", "new/child.txt")
                .startsWith(sessionRoot));
    }

    @Test
    void rejectsEverySymlinkComponentEvenWhenTheLexicalPathStaysInsideTheSession()
            throws IOException {
        GuardedWorkspacePathResolver resolver = new GuardedWorkspacePathResolver(root, 32);
        Path outside = Files.createDirectories(root.resolve("outside"));
        Path sessionRoot = resolver.sessionRoot("runtime-session");
        Files.createSymbolicLink(sessionRoot.resolve("link"), outside);

        assertThrows(SecurityException.class,
                () -> resolver.resolve("runtime-session", "link/secret.txt"));
    }

    @Test
    void workspaceToolsRouteEachCallByRuntimeSessionAndRejectOversizedUtf8BeforeWriting()
            throws IOException {
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(root.toString());
        config.getWorkspace().setTrustedLocal(true);
        config.getWorkspace().setMaxFileBytes(4);
        WorkspaceFileTools tools = new WorkspaceFileTools(
                new GuardedWorkspacePathResolver(root, 4), config);
        RuntimeContext first = AgentTestContexts.runtimeContext(
                AgentTestContexts.liteFlowContext());

        assertEquals("ok", tools.writeFile(first, "note.txt", "四"));
        assertEquals("四", tools.readFile(first, "note.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> tools.writeFile(first, "too-large.txt", "四四"));

        RuntimeContext second = runtimeContext("other-runtime-session");
        assertEquals("ok", tools.writeFile(second, "note.txt", "B"));
        assertEquals("四", tools.readFile(first, "note.txt"));
        assertEquals("B", tools.readFile(second, "note.txt"));
    }

    private static RuntimeContext runtimeContext(String runtimeSessionId) {
        Slot slot = new Slot();
        slot.setChainId("chain-2");
        slot.setConversationId("conversation-2");
        slot.putRequestId("request-2");
        LiteFlowAgentContext context = new LiteFlowAgentContext(
                new AgentInvocationIdentity(
                        "namespace-2",
                        "user-2",
                        "conversation-2",
                        "agent-2",
                        "runtime-user-2",
                        runtimeSessionId,
                        "agent-namespace-2"),
                slot,
                "chain-2",
                "node-2",
                "request-2",
                "trace-2",
                Instant.parse("2030-01-01T00:00:00Z"),
                AgentOutputSpec.text(),
                LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + "second");
        return RuntimeContext.builder()
                .userId(context.getRuntimeUserId())
                .sessionId(context.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, context)
                .put(Slot.class, slot)
                .build();
    }
}

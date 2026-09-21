package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.LocalExecutionFilesystem;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.*;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HarnessLocalShellTest {
    @TempDir Path root;

    private AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.setApplicationName("local-shell-test");
        config.getHarness().getLocal().setWorkspaceRoot(root.toString());
        config.getSessionStore().setJsonWorkspaceRoot(root.toString());
        config.getSkills().setEnabled(false);
        return config;
    }

    @Test void oneComponentHookControlsOnlyTheSessionAwareTool() {
        AgentConfig config = config();
        try (HarnessAgentRuntime disabled = new Component(false).build(config)) {
            assertFalse(disabled.agent().getToolkit().getToolNames().contains("execute"));
            assertFalse(disabled.agent().getToolkit().getToolNames().contains("execute_shell_command"));
        }
        try (HarnessAgentRuntime enabled = new Component().build(config)) {
            assertTrue(enabled.agent().getToolkit().getToolNames().contains("execute"));
            assertFalse(enabled.agent().getToolkit().getToolNames().contains("execute_shell_command"));
            assertInstanceOf(LocalExecutionFilesystem.class, enabled.agent().getWorkspaceManager().getFilesystem());
            assertEquals(root.toAbsolutePath().resolve(config.getApplicationName()), enabled.agent().getWorkspaceManager().getWorkspace());
        }
    }

    @Test void enabledLocalShellRequiresRootAndTheExistingWhitelistPolicy() {
        AgentConfig config = config();
        config.getHarness().getLocal().setWorkspaceRoot(null);
        config.getSessionStore().setJsonWorkspaceRoot(null);
        assertThrows(AgentConfigException.class, () -> new Component(true).build(config));
        config.getHarness().getLocal().setWorkspaceRoot(root.toString());
        config.getSessionStore().setJsonWorkspaceRoot(root.toString());
        config.getHarness().getShell().setMode(ShellMode.DISABLED);
        assertThrows(AgentConfigException.class, () -> new Component(true).build(config));
        config.getHarness().getShell().setMode(ShellMode.WHITELIST);
        config.getHarness().getShell().setWhitelist(List.of());
        assertThrows(AgentConfigException.class, () -> new Component(true).build(config));
    }

    @Test void missingRootFailsBeforeOpeningMysqlStorage() {
        AgentConfig config = config();
        config.getSessionStore().setType(AgentSessionStoreType.MYSQL);
        config.getHarness().getLocal().setWorkspaceRoot(null);
        config.getSessionStore().setJsonWorkspaceRoot(null);
        var failure = assertThrows(AgentConfigException.class, () -> new Component(true).build(config));
        assertTrue(failure.getMessage().contains("workspace-root"));
    }

    @Test void dockerDefaultsToContainerShellAndExplicitFalseDisablesIt() {
        AgentConfig config = config();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        config.getHarness().getDocker().setWorkspaceProjectionEnabled(false);
        try (HarnessAgentRuntime runtime = new Component().build(config)) {
            assertTrue(runtime.agent().getToolkit().getToolNames().contains("execute"));
            assertFalse(runtime.agent().getToolkit().getToolNames().contains("execute_shell_command"));
        }
        config.getHarness().getShell().setMode(ShellMode.DISABLED);
        try (HarnessAgentRuntime runtime = new Component(false).build(config)) {
            assertFalse(runtime.agent().getToolkit().getToolNames().contains("execute"));
            assertFalse(runtime.agent().getToolkit().getToolNames().contains("execute_shell_command"));
        }
    }

    @Test void dockerIgnoresInvalidLocalExecutionRoot() {
        AgentConfig config = config();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        config.getHarness().getDocker().setWorkspaceProjectionEnabled(false);
        String records = config.getSessionStore().getJsonWorkspaceRoot();
        config.getHarness().getLocal().setWorkspaceRoot("invalid" + (char) 0 + "host-root");
        try (HarnessAgentRuntime runtime = new Component().build(config)) {
            assertEquals(Path.of(records).toAbsolutePath().normalize(), runtime.agent().getWorkspaceManager().getWorkspace());
        }
    }

    private static final class Component extends HarnessAgentComponent {
        private final Boolean shell;
        Component() { this.shell = null; }
        Component(boolean shell) { this.shell = shell; }
        HarnessAgentRuntime build(AgentConfig config) {
            return buildRuntime(new AgentRuntimeBuildContext(config, "test", "test", "lf-" + "a".repeat(64)));
        }
        @Override protected boolean enableShellTool() { return shell == null ? super.enableShellTool() : shell; }
        @Override protected String systemPrompt() { return "Reply briefly"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "hello"; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("test model"); }
        @Override protected AgentStateStoreResolver stateStoreResolver() {
            return ignored -> new ResolvedAgentStateStore(new InMemoryAgentStateStore(), true);
        }
        @Override protected Model buildModel() {
            return new Model() {
                public String getModelName() { return "offline"; }
                public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                    return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder().text("ok").build())).build());
                }
            };
        }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder.disableDefaultWorkspaceSkills().disableDynamicSkills().disableToolsConfig();
        }
    }
}

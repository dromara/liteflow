package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.workspace.WorkspaceIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessAgentBuilderFilesystemBridgeTest {

    @Test
    void upstreamRejectsAnAbstractFilesystemSentinelCombinedWithAPublicSpec() {
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .abstractFilesystem(filesystem())
                .filesystem(new LocalFilesystemSpec());

        IllegalStateException failure = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void acceptsExactlyOneAbstractFilesystemOrPublicFilesystemSpec() {
        assertDoesNotThrow(() -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                HarnessAgent.builder().abstractFilesystem(filesystem())));
        assertDoesNotThrow(() -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                HarnessAgent.builder().filesystem(new RemoteFilesystemSpec())));
    }

    @Test
    void rejectsNoOpAndMultipleFilesystemSelectionsBeforeBuild() {
        AgentConfigException missing = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                        HarnessAgent.builder()));
        assertTrue(missing.getMessage().contains("explicit filesystem backend"));

        AgentConfigException multiple = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.requireExactlyOne(
                        HarnessAgent.builder()
                                .abstractFilesystem(filesystem())
                                .filesystem(new RemoteFilesystemSpec())));
        assertTrue(multiple.getMessage().contains("exactly one"));
    }

    @Test
    void verifiesPinnedMetadataAndFailsClosedOnBuilderShapeDrift() {
        assertDoesNotThrow(HarnessAgentBuilderFilesystemBridge::verifyContract);

        AgentConfigException drift = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.validateShape(DriftedBuilder.class));
        assertTrue(drift.getMessage().contains("2.0.3"));
        assertNotNull(drift.getCause());
    }

    @Test
    void taskOwnershipInspectionIsVersionPinnedAndMatchesUpstreamBuilderBranches() {
        var dynamic = HarnessAgentBuilderTaskOwnershipBridge.snapshot(
                HarnessAgent.builder().model(model()), null);
        var staticSubagents = HarnessAgentBuilderTaskOwnershipBridge.snapshot(
                HarnessAgent.builder().model(model()).disableDynamicSubagents(), null);
        var disabled = HarnessAgentBuilderTaskOwnershipBridge.snapshot(
                HarnessAgent.builder().model(model()).disableSubagents(), null);

        assertEquals(
                HarnessAgentBuilderTaskOwnershipBridge.BuiltInSubagents.DYNAMIC,
                dynamic.builtInSubagents());
        assertEquals(
                HarnessAgentBuilderTaskOwnershipBridge.BuiltInSubagents.STATIC,
                staticSubagents.builtInSubagents());
        assertEquals(
                HarnessAgentBuilderTaskOwnershipBridge.BuiltInSubagents.NONE,
                disabled.builtInSubagents());

        AgentConfigException drift = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderTaskOwnershipBridge.validateShape(
                        DriftedBuilder.class));
        assertTrue(drift.getMessage().contains("2.0.3"));
        assertNotNull(drift.getCause());
    }

    @Test
    void dynamicSubagentPermissionDecoratorPinsPrivateFinalHarnessFields() {
        List<Field> fields = HarnessAgentBuilderSubagentPermissionBridge
                .validateDynamicMiddlewareShape(
                        io.agentscope.harness.agent.middleware.DynamicSubagentsMiddleware.class);

        assertEquals(List.of("staticEntries", "factoryBuilder"),
                fields.stream().map(Field::getName).toList());
        assertEquals(List.of(List.class, Function.class),
                fields.stream().map(Field::getType).toList());
        assertTrue(fields.stream().allMatch(field ->
                java.lang.reflect.Modifier.isPrivate(field.getModifiers())
                        && java.lang.reflect.Modifier.isFinal(field.getModifiers())));

        AgentConfigException drift = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderSubagentPermissionBridge
                        .validateDynamicMiddlewareShape(DriftedDynamicMiddleware.class));
        assertTrue(drift.getMessage().contains("agentscope-harness 2.0.3"));
    }

    @Test
    void upstreamAllocatesAnOpenWorkspaceIndexBeforeRejectingRemoteSpecWithoutStore(
            @TempDir Path workspace) throws Exception {
        RemoteFilesystemSpec spec = new RemoteFilesystemSpec();
        WorkspaceIndex index = null;
        try {
            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> HarnessAgent.builder()
                            .workspace(workspace)
                            .stateStore(stateStore())
                            .filesystem(spec)
                            .build());

            assertTrue(failure.getMessage().contains("no BaseStore"));
            index = workspaceIndex(spec);
            assertNotNull(index);
            Connection connection = connection(index);
            assertFalse(connection.isClosed());
            assertTrue(Files.isRegularFile(workspace.resolve(".index/workspace.db")));
        }
        finally {
            if (index == null) {
                index = workspaceIndex(spec);
            }
            if (index != null) {
                index.close();
            }
        }
    }

    @Test
    void remotePreflightUsesTheUpstreamStoreOrDistributedStoreCondition() {
        HarnessAgent.Builder missingStore = HarnessAgent.builder()
                .filesystem(new RemoteFilesystemSpec());
        var missingSnapshot = HarnessAgentBuilderFilesystemBridge.snapshot(missingStore);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class,
                () -> HarnessAgentBuilderFilesystemBridge.preflightKnownBuildFailures(
                        missingStore, missingSnapshot));
        assertTrue(failure.getMessage().contains("RemoteFilesystemSpec"));

        DistributedStore distributedStore = DistributedStore.builder()
                .agentStateStore(stateStore())
                .baseStore(baseStore())
                .build();
        HarnessAgent.Builder autoWired = HarnessAgent.builder()
                .filesystem(new RemoteFilesystemSpec())
                .distributedStore(distributedStore);
        var autoWiredSnapshot = HarnessAgentBuilderFilesystemBridge.snapshot(autoWired);

        assertDoesNotThrow(
                () -> HarnessAgentBuilderFilesystemBridge.preflightKnownBuildFailures(
                        autoWired, autoWiredSnapshot));
    }

    private static AbstractFilesystem filesystem() {
        return (AbstractFilesystem) Proxy.newProxyInstance(
                AbstractFilesystem.class.getClassLoader(),
                new Class<?>[] {AbstractFilesystem.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("filesystem must not be invoked");
                });
    }

    private static io.agentscope.core.state.AgentStateStore stateStore() {
        return (io.agentscope.core.state.AgentStateStore) Proxy.newProxyInstance(
                io.agentscope.core.state.AgentStateStore.class.getClassLoader(),
                new Class<?>[] {io.agentscope.core.state.AgentStateStore.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("state store must not be invoked");
                });
    }

    private static Model model() {
        return (Model) Proxy.newProxyInstance(
                Model.class.getClassLoader(),
                new Class<?>[] {Model.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("model must not be invoked");
                });
    }

    private static BaseStore baseStore() {
        return (BaseStore) Proxy.newProxyInstance(
                BaseStore.class.getClassLoader(),
                new Class<?>[] {BaseStore.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("base store must not be invoked");
                });
    }

    private static WorkspaceIndex workspaceIndex(RemoteFilesystemSpec spec) throws Exception {
        Field field = RemoteFilesystemSpec.class.getDeclaredField("workspaceIndex");
        assertTrue(field.trySetAccessible());
        return (WorkspaceIndex) field.get(spec);
    }

    private static Connection connection(WorkspaceIndex index) throws Exception {
        Field field = WorkspaceIndex.class.getDeclaredField("conn");
        assertTrue(field.trySetAccessible());
        return (Connection) field.get(index);
    }

    private static final class DriftedBuilder {
    }

    private static final class DriftedDynamicMiddleware {
        @SuppressWarnings("unused")
        private final List<Object> staticEntries = List.of();
    }
}

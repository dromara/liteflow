package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Read-only, version-pinned inspection of mandatory Harness builder invariants. */
final class HarnessAgentBuilderFilesystemBridge {

    private static final String AGENTSCOPE_HARNESS_VERSION = "2.0.2";
    private static final String VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final List<FieldContract> FIELD_CONTRACTS = List.of(
            new FieldContract("abstractFilesystem", AbstractFilesystem.class),
            new FieldContract("sandboxFilesystemSpec", SandboxFilesystemSpec.class),
            new FieldContract("remoteFilesystemSpec", RemoteFilesystemSpec.class),
            new FieldContract("localFilesystemSpec", LocalFilesystemSpec.class),
            new FieldContract("workspace", Path.class),
            new FieldContract("toolkit", Toolkit.class),
            new FieldContract("distributedStore", DistributedStore.class));
    private static volatile List<Field> builderFields;

    private HarnessAgentBuilderFilesystemBridge() {
    }

    static void verifyContract() {
        builderFields();
    }

    static void requireExactlyOne(HarnessAgent.Builder builder) {
        snapshot(builder);
    }

    static FilesystemSnapshot snapshot(HarnessAgent.Builder builder) {
        if (builder == null) {
            throw incompatible("builder must not be null");
        }
        try {
            String selectedField = null;
            Object selectedValue = null;
            List<Field> fields = builderFields();
            for (int index = 0; index < 4; index++) {
                Field field = fields.get(index);
                Object value = field.get(builder);
                if (value != null) {
                    if (selectedField != null) {
                        throw new AgentConfigException(
                                "Harness filesystemConfigurer must select exactly one filesystem"
                                        + " backend");
                    }
                    selectedField = field.getName();
                    selectedValue = value;
                }
            }
            if (selectedField == null) {
                throw new AgentConfigException(
                        "Harness filesystemConfigurer must install an explicit filesystem backend");
            }
            return new FilesystemSnapshot(
                    selectedField, selectedValue, (Path) fields.get(4).get(builder));
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder filesystem fields", failure);
        }
    }

    static ToolkitSnapshot snapshotToolkit(HarnessAgent.Builder builder, Toolkit expected) {
        if (builder == null) {
            throw incompatible("builder must not be null");
        }
        Objects.requireNonNull(expected, "expected");
        try {
            Toolkit actual = (Toolkit) builderFields().get(5).get(builder);
            if (actual != expected) {
                throw new AgentConfigException(
                        "HarnessAgent.Builder must retain the prepared serial Toolkit identity");
            }
            return new ToolkitSnapshot(expected);
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder toolkit field", failure);
        }
    }

    static void preflightKnownBuildFailures(
            HarnessAgent.Builder builder, FilesystemSnapshot filesystem) {
        Objects.requireNonNull(filesystem, "filesystem");
        if (!(filesystem.selectedValue() instanceof RemoteFilesystemSpec remote)
                || remote.hasStore()) {
            return;
        }
        try {
            DistributedStore distributedStore =
                    (DistributedStore) builderFields().get(6).get(builder);
            if (distributedStore == null || distributedStore.baseStore() == null) {
                throw new AgentConfigException(
                        "RemoteFilesystemSpec requires a BaseStore or DistributedStore"
                                + " before Harness runtime build");
            }
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder distributedStore field", failure);
        }
        catch (AgentConfigException failure) {
            throw failure;
        }
        catch (RuntimeException failure) {
            throw new AgentConfigException(
                    "cannot resolve DistributedStore BaseStore before Harness runtime build",
                    failure);
        }
    }

    static List<Field> validateShape(Class<?> builderType) {
        List<Field> fields = new ArrayList<>(FIELD_CONTRACTS.size());
        try {
            for (FieldContract contract : FIELD_CONTRACTS) {
                Field field = builderType.getDeclaredField(contract.name());
                int modifiers = field.getModifiers();
                if (Modifier.isPublic(modifiers)
                        || Modifier.isProtected(modifiers)
                        || Modifier.isPrivate(modifiers)
                        || Modifier.isStatic(modifiers)
                        || Modifier.isFinal(modifiers)
                        || field.getType() != contract.type()) {
                    throw incompatible(
                            "HarnessAgent.Builder." + contract.name() + " signature changed");
                }
                if (!field.trySetAccessible()) {
                    throw incompatible(
                            "HarnessAgent.Builder." + contract.name() + " is inaccessible");
                }
                fields.add(field);
            }
            return List.copyOf(fields);
        }
        catch (NoSuchFieldException failure) {
            throw incompatible("HarnessAgent.Builder filesystem shape changed", failure);
        }
    }

    private static List<Field> builderFields() {
        List<Field> resolved = builderFields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderFilesystemBridge.class) {
            if (builderFields == null) {
                requireHarnessVersion();
                builderFields = validateShape(HarnessAgent.Builder.class);
            }
            return builderFields;
        }
    }

    private static void requireHarnessVersion() {
        Properties properties = new Properties();
        try (var input = HarnessAgent.class.getClassLoader()
                .getResourceAsStream(VERSION_RESOURCE)) {
            if (input == null) {
                throw incompatible("AgentScope Harness version metadata is unavailable");
            }
            properties.load(input);
        }
        catch (Exception failure) {
            if (failure instanceof AgentConfigException configFailure) {
                throw configFailure;
            }
            throw incompatible("cannot read AgentScope Harness version metadata", failure);
        }
        String version = properties.getProperty("version");
        if (!AGENTSCOPE_HARNESS_VERSION.equals(version)) {
            throw incompatible("unexpected AgentScope Harness version " + version);
        }
    }

    private static AgentConfigException incompatible(String detail) {
        return incompatible(detail, null);
    }

    private static AgentConfigException incompatible(String detail, Throwable cause) {
        return new AgentConfigException(
                "Harness filesystem guard requires agentscope-harness 2.0.2: " + detail,
                cause);
    }

    private record FieldContract(String name, Class<?> type) {
    }

    record FilesystemSnapshot(String selectedField, Object selectedValue, Path workspace) {

        FilesystemSnapshot {
            Objects.requireNonNull(selectedField, "selectedField");
            Objects.requireNonNull(selectedValue, "selectedValue");
        }

        void requireUnchanged(HarnessAgent.Builder builder) {
            FilesystemSnapshot actual = HarnessAgentBuilderFilesystemBridge.snapshot(builder);
            if (!selectedField.equals(actual.selectedField)
                    || selectedValue != actual.selectedValue) {
                throw new AgentConfigException(
                        "customizeHarness must retain the configured filesystem identity");
            }
            if (!Objects.equals(workspace, actual.workspace)) {
                throw new AgentConfigException(
                        "customizeHarness must retain the configured workspace");
            }
        }
    }

    record ToolkitSnapshot(Toolkit toolkit) {

        ToolkitSnapshot {
            Objects.requireNonNull(toolkit, "toolkit");
        }

        void requireUnchanged(HarnessAgent.Builder builder) {
            HarnessAgentBuilderFilesystemBridge.snapshotToolkit(builder, toolkit);
        }
    }
}

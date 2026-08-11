package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Read-only, version-pinned inspection of Harness builder filesystem selection. */
final class HarnessAgentBuilderFilesystemBridge {

    private static final String AGENTSCOPE_HARNESS_VERSION = "2.0.2";
    private static final String VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final List<FieldContract> FIELD_CONTRACTS = List.of(
            new FieldContract("abstractFilesystem", AbstractFilesystem.class),
            new FieldContract("sandboxFilesystemSpec", SandboxFilesystemSpec.class),
            new FieldContract("remoteFilesystemSpec", RemoteFilesystemSpec.class),
            new FieldContract("localFilesystemSpec", LocalFilesystemSpec.class));
    private static volatile List<Field> filesystemFields;

    private HarnessAgentBuilderFilesystemBridge() {
    }

    static void verifyContract() {
        filesystemFields();
    }

    static void requireExactlyOne(HarnessAgent.Builder builder) {
        if (builder == null) {
            throw incompatible("builder must not be null");
        }
        int configured = 0;
        try {
            for (Field field : filesystemFields()) {
                if (field.get(builder) != null) {
                    configured++;
                }
            }
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder filesystem fields", failure);
        }
        if (configured == 0) {
            throw new AgentConfigException(
                    "Harness filesystemConfigurer must install an explicit filesystem backend");
        }
        if (configured != 1) {
            throw new AgentConfigException(
                    "Harness filesystemConfigurer must select exactly one filesystem backend");
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

    private static List<Field> filesystemFields() {
        List<Field> resolved = filesystemFields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderFilesystemBridge.class) {
            if (filesystemFields == null) {
                requireHarnessVersion();
                filesystemFields = validateShape(HarnessAgent.Builder.class);
            }
            return filesystemFields;
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
}

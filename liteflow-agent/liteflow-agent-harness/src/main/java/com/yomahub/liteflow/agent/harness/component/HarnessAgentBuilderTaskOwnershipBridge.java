package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.subagent.task.TaskRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Read-only, version-pinned inspection of Harness task-repository ownership decisions. */
final class HarnessAgentBuilderTaskOwnershipBridge {

    private static final String AGENTSCOPE_HARNESS_VERSION = "2.0.3";
    private static final String VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final List<FieldContract> FIELD_CONTRACTS = List.of(
            new FieldContract("taskRepository", TaskRepository.class),
            new FieldContract("leafSubagent", boolean.class),
            new FieldContract("disableSubagents", boolean.class),
            new FieldContract("disableDynamicSubagents", boolean.class),
            new FieldContract("model", Model.class));
    private static volatile List<Field> builderFields;

    private HarnessAgentBuilderTaskOwnershipBridge() {
    }

    static TaskOwnershipSnapshot snapshot(
            HarnessAgent.Builder builder, TaskRepository expectedRepository) {
        Objects.requireNonNull(builder, "builder");
        try {
            List<Field> fields = builderFields();
            TaskRepository actualRepository = (TaskRepository) fields.get(0).get(builder);
            if (actualRepository != expectedRepository) {
                throw new AgentConfigException(
                        "customizeHarness must retain the configured TaskRepository identity");
            }
            boolean leafSubagent = fields.get(1).getBoolean(builder);
            boolean disableSubagents = fields.get(2).getBoolean(builder);
            boolean disableDynamicSubagents = fields.get(3).getBoolean(builder);
            Model model = (Model) fields.get(4).get(builder);
            BuiltInSubagents builtInSubagents;
            if (leafSubagent || disableSubagents || model == null) {
                builtInSubagents = BuiltInSubagents.NONE;
            }
            else if (disableDynamicSubagents) {
                builtInSubagents = BuiltInSubagents.STATIC;
            }
            else {
                builtInSubagents = BuiltInSubagents.DYNAMIC;
            }
            return new TaskOwnershipSnapshot(actualRepository, builtInSubagents);
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder task ownership fields", failure);
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
            throw incompatible("HarnessAgent.Builder task ownership shape changed", failure);
        }
    }

    private static List<Field> builderFields() {
        List<Field> resolved = builderFields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderTaskOwnershipBridge.class) {
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
                "Harness task ownership guard requires agentscope-harness 2.0.3: " + detail,
                cause);
    }

    private record FieldContract(String name, Class<?> type) {
    }

    enum BuiltInSubagents {
        NONE,
        STATIC,
        DYNAMIC
    }

    record TaskOwnershipSnapshot(
            TaskRepository taskRepository, BuiltInSubagents builtInSubagents) {

        TaskOwnershipSnapshot {
            Objects.requireNonNull(builtInSubagents, "builtInSubagents");
        }

        boolean harnessWillShutdownWorkspaceTasks() {
            return builtInSubagents != BuiltInSubagents.NONE;
        }
    }
}

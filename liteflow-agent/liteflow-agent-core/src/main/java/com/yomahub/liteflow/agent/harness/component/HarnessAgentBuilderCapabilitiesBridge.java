package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.task.TaskRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Version-pinned, read-only guard for LiteFlow-managed Harness capability configuration. */
final class HarnessAgentBuilderCapabilitiesBridge {

    private static final String AGENTSCOPE_HARNESS_VERSION = "2.0.3";
    private static final String VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final List<FieldContract> FIELD_CONTRACTS = List.of(
            new FieldContract("compactionConfig", CompactionConfig.class),
            new FieldContract("disableCompaction", boolean.class),
            new FieldContract("memoryConfig", MemoryConfig.class),
            new FieldContract("toolResultEvictionConfig", ToolResultEvictionConfig.class),
            new FieldContract("disableToolResultEviction", boolean.class),
            new FieldContract("additionalContextFiles", List.class),
            new FieldContract("skillRepositories", List.class),
            new FieldContract("skillFilter", SkillFilter.class),
            new FieldContract("subagentDeclarations", List.class),
            new FieldContract("taskRepository", TaskRepository.class),
            new FieldContract("planModeEnabled", boolean.class),
            new FieldContract("planModeAllowShell", boolean.class));
    private static volatile List<Field> builderFields;

    private HarnessAgentBuilderCapabilitiesBridge() {
    }

    static CapabilitiesSnapshot snapshot(HarnessAgent.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        try {
            List<Field> fields = builderFields();
            return new CapabilitiesSnapshot(
                    (CompactionConfig) fields.get(0).get(builder),
                    fields.get(1).getBoolean(builder),
                    (MemoryConfig) fields.get(2).get(builder),
                    (ToolResultEvictionConfig) fields.get(3).get(builder),
                    fields.get(4).getBoolean(builder),
                    copyList(fields.get(5).get(builder), String.class),
                    copyList(fields.get(6).get(builder), AgentSkillRepository.class),
                    (SkillFilter) fields.get(7).get(builder),
                    copyList(fields.get(8).get(builder), SubagentDeclaration.class),
                    (TaskRepository) fields.get(9).get(builder),
                    fields.get(10).getBoolean(builder),
                    fields.get(11).getBoolean(builder));
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect HarnessAgent.Builder capabilities", failure);
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
                        || field.getType() != contract.type()) {
                    throw incompatible(
                            "HarnessAgent.Builder." + contract.name() + " signature changed");
                }
                if (!field.trySetAccessible()) {
                    throw incompatible("HarnessAgent.Builder." + contract.name() + " is inaccessible");
                }
                fields.add(field);
            }
            return List.copyOf(fields);
        }
        catch (NoSuchFieldException failure) {
            throw incompatible("HarnessAgent.Builder capability shape changed", failure);
        }
    }

    private static List<Field> builderFields() {
        List<Field> resolved = builderFields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderCapabilitiesBridge.class) {
            if (builderFields == null) {
                requireHarnessVersion();
                builderFields = validateShape(HarnessAgent.Builder.class);
            }
            return builderFields;
        }
    }

    private static void requireHarnessVersion() {
        Properties properties = new Properties();
        try (var input = HarnessAgent.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE)) {
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

    private static <T> List<T> copyList(Object value, Class<T> elementType) {
        if (!(value instanceof List<?> list)) {
            throw incompatible("HarnessAgent.Builder capability list shape changed");
        }
        List<T> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!elementType.isInstance(element)) {
                throw incompatible("HarnessAgent.Builder capability list element changed");
            }
            result.add(elementType.cast(element));
        }
        return List.copyOf(result);
    }

    private static void requireIdentityPrefix(
            String name, List<?> expected, List<?> actual) {
        if (actual.size() < expected.size()) {
            throw changed(name);
        }
        for (int index = 0; index < expected.size(); index++) {
            if (actual.get(index) != expected.get(index)) {
                throw changed(name);
            }
        }
    }

    private static void requireValuePrefix(
            String name, List<String> expected, List<String> actual) {
        if (actual.size() < expected.size()
                || !actual.subList(0, expected.size()).equals(expected)) {
            throw changed(name);
        }
    }

    private static AgentConfigException changed(String name) {
        return new AgentConfigException(
                "customizeHarness must retain LiteFlow-managed Harness capability " + name);
    }

    private static AgentConfigException incompatible(String detail) {
        return incompatible(detail, null);
    }

    private static AgentConfigException incompatible(String detail, Throwable cause) {
        return new AgentConfigException(
                "Harness capability guard requires agentscope-harness 2.0.3: " + detail,
                cause);
    }

    private record FieldContract(String name, Class<?> type) {
    }

    record CapabilitiesSnapshot(
            CompactionConfig compaction,
            boolean compactionDisabled,
            MemoryConfig memory,
            ToolResultEvictionConfig eviction,
            boolean evictionDisabled,
            List<String> additionalContextFiles,
            List<AgentSkillRepository> skillRepositories,
            SkillFilter skillFilter,
            List<SubagentDeclaration> subagents,
            TaskRepository taskRepository,
            boolean planModeEnabled,
            boolean planModeAllowShell) {

        void requireUnchanged(
                HarnessAgent.Builder builder,
                boolean protectCompaction,
                boolean protectMemory,
                boolean protectEviction,
                boolean protectPlanMode) {
            CapabilitiesSnapshot actual = snapshot(builder);
            if (protectCompaction
                    && (actual.compaction != compaction
                            || actual.compactionDisabled != compactionDisabled)) {
                throw changed("compaction");
            }
            if (protectMemory && actual.memory != memory) {
                throw changed("memory");
            }
            if (protectEviction
                    && (actual.eviction != eviction
                            || actual.evictionDisabled != evictionDisabled)) {
                throw changed("tool result eviction");
            }
            if (!additionalContextFiles.isEmpty()) {
                requireValuePrefix(
                        "additional context files",
                        additionalContextFiles,
                        actual.additionalContextFiles);
            }
            if (!skillRepositories.isEmpty()) {
                requireIdentityPrefix(
                        "skill repositories", skillRepositories, actual.skillRepositories);
            }
            if (actual.skillFilter != skillFilter) {
                throw changed("skill filter");
            }
            if (!subagents.isEmpty()) {
                requireIdentityPrefix("subagents", subagents, actual.subagents);
            }
            // TaskRepository identity is guarded by HarnessAgentBuilderTaskOwnershipBridge,
            // which also owns the shutdown-transfer decision.
            if (protectPlanMode
                    && (actual.planModeEnabled != planModeEnabled
                            || actual.planModeAllowShell != planModeAllowShell)) {
                throw changed("plan mode");
            }
        }
    }
}

package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.harness.agent.HarnessAgent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Version-pinned fail-closed policy and inspection of the final Harness permission context. */
final class HarnessAgentBuilderPermissionBridge {

    private static final String AGENTSCOPE_VERSION = "2.0.2";
    private static final String HARNESS_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-harness/pom.properties";
    private static final String CORE_VERSION_RESOURCE =
            "META-INF/maven/io.agentscope/agentscope-core/pom.properties";
    private static final List<String> DANGEROUS_HARNESS_TOOLS =
            List.of("execute", "write_file", "edit_file");
    private static volatile Fields fields;

    private HarnessAgentBuilderPermissionBridge() {
    }

    /**
     * Makes the upstream 2.0.2 {@code DEFAULT} contract explicit without changing filesystem
     * boundaries: its documented fallback is ASK, but a completely empty state selects a legacy
     * lightweight path. Exact ASK rules for Harness' execute/write tools engage the full engine.
     */
    static PermissionContextState failClosed(PermissionContextState configured) {
        if (configured != null && configured.getMode() == PermissionMode.BYPASS) {
            throw new AgentConfigException("Harness permission mode BYPASS is prohibited");
        }
        if (configured != null && !configured.isTrivial()) {
            return configured;
        }
        PermissionContextState.Builder builder =
                PermissionContextState.builder().mode(PermissionMode.DEFAULT);
        for (String toolName : DANGEROUS_HARNESS_TOOLS) {
            builder.addAskRule(
                    toolName,
                    new PermissionRule(
                            toolName,
                            null,
                            PermissionBehavior.ASK,
                            "liteflowHarnessDefault"));
        }
        return builder.build();
    }

    static PermissionSnapshot snapshot(
            HarnessAgent.Builder builder, PermissionContextState expected) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(expected, "expected");
        try {
            Fields resolved = fields();
            Object inner = resolved.inner().get(builder);
            PermissionContextState actual =
                    (PermissionContextState) resolved.permissionContext().get(inner);
            requireExpected(actual, expected);
            return new PermissionSnapshot(inner, expected);
        }
        catch (IllegalAccessException failure) {
            throw incompatible("cannot inspect final permission context", failure);
        }
    }

    private static void requireExpected(
            PermissionContextState actual, PermissionContextState expected) {
        if (actual != expected) {
            throw new AgentConfigException(
                    "customizeHarness must retain the final permission context identity");
        }
        if (actual.getMode() == PermissionMode.BYPASS) {
            throw new AgentConfigException("Harness permission mode BYPASS is prohibited");
        }
    }

    private static Fields fields() {
        Fields resolved = fields;
        if (resolved != null) {
            return resolved;
        }
        synchronized (HarnessAgentBuilderPermissionBridge.class) {
            if (fields == null) {
                requireVersion(HarnessAgent.class, HARNESS_VERSION_RESOURCE, "Harness");
                requireVersion(ReActAgent.class, CORE_VERSION_RESOURCE, "core");
                fields = validateShape();
            }
            return fields;
        }
    }

    private static Fields validateShape() {
        try {
            Field inner = HarnessAgent.Builder.class.getDeclaredField("inner");
            int innerModifiers = inner.getModifiers();
            if (!Modifier.isPrivate(innerModifiers)
                    || !Modifier.isFinal(innerModifiers)
                    || Modifier.isStatic(innerModifiers)
                    || inner.getType() != ReActAgent.Builder.class
                    || !inner.trySetAccessible()) {
                throw incompatible("HarnessAgent.Builder.inner signature changed");
            }
            Field permission =
                    ReActAgent.Builder.class.getDeclaredField("permissionContext");
            int permissionModifiers = permission.getModifiers();
            if (!Modifier.isPrivate(permissionModifiers)
                    || Modifier.isFinal(permissionModifiers)
                    || Modifier.isStatic(permissionModifiers)
                    || permission.getType() != PermissionContextState.class
                    || !permission.trySetAccessible()) {
                throw incompatible("ReActAgent.Builder.permissionContext signature changed");
            }
            return new Fields(inner, permission);
        }
        catch (NoSuchFieldException failure) {
            throw incompatible("AgentScope permission builder shape changed", failure);
        }
    }

    private static void requireVersion(Class<?> owner, String resource, String artifact) {
        Properties properties = new Properties();
        try (var input = owner.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw incompatible("AgentScope " + artifact + " version metadata is unavailable");
            }
            properties.load(input);
        }
        catch (Exception failure) {
            if (failure instanceof AgentConfigException configFailure) {
                throw configFailure;
            }
            throw incompatible(
                    "cannot read AgentScope " + artifact + " version metadata", failure);
        }
        String version = properties.getProperty("version");
        if (!AGENTSCOPE_VERSION.equals(version)) {
            throw incompatible("unexpected AgentScope " + artifact + " version " + version);
        }
    }

    private static AgentConfigException incompatible(String detail) {
        return incompatible(detail, null);
    }

    private static AgentConfigException incompatible(String detail, Throwable cause) {
        return new AgentConfigException(
                "Harness permission guard requires AgentScope 2.0.2: " + detail, cause);
    }

    private record Fields(Field inner, Field permissionContext) {
    }

    record PermissionSnapshot(Object inner, PermissionContextState permissionContext) {

        PermissionSnapshot {
            Objects.requireNonNull(inner, "inner");
            Objects.requireNonNull(permissionContext, "permissionContext");
        }

        void requireUnchanged(HarnessAgent.Builder builder) {
            Objects.requireNonNull(builder, "builder");
            try {
                Fields resolved = fields();
                Object actualInner = resolved.inner().get(builder);
                if (actualInner != inner) {
                    throw new AgentConfigException(
                            "customizeHarness must retain the Harness permission builder identity");
                }
                PermissionContextState actual =
                        (PermissionContextState) resolved.permissionContext().get(actualInner);
                requireExpected(actual, permissionContext);
            }
            catch (IllegalAccessException failure) {
                throw incompatible("cannot re-inspect final permission context", failure);
            }
        }
    }
}
